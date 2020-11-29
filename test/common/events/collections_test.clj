(ns common.events.collections-test
  (:require [common.events.collections :as sut]
            [clojure.string :as str]
            [clojure.test :as t]))


(t/deftest collection-events
       (let [col [{:id    "1"
                   :name  "John"
                   :score {:level 4}
                   :age   34}
                  {:id    "2"
                   :name  "Hannah"
                   :score {:level 3}
                   :age   33}
                  {:id    "3"
                   :name  "Charlie"
                   :score {:level 2}
                   :age   10}
                  {:id    "4"
                   :name  "Leo"
                   :age   19
                   :score {:level 1}}]
             db (-> {}
                    (sut/add-collection [nil :persons :id col])
                    (sut/add-sort       [nil :persons :name-asc (partial sut/asc :name :id)])
                    (sut/add-sort       [nil :persons :name-desc (partial sut/desc :name :id)])
                    (sut/add-sort       [nil :persons :level-asc (partial sut/asc (comp :level :score) :id)])
                    (sut/add-sort       [nil :persons :level-desc (partial sut/desc (comp :level :score) :id)])
                    (sut/add-sort       [nil :persons :age-asc (partial sut/asc :age :id)])
                    (sut/add-sort       [nil :persons :age-desc (partial sut/desc :age :id)]))]

         (t/is (= col
                  (sut/get-sort db [nil :persons :default])))

         (t/is (= (sut/get-sort db [nil :persons :name-asc])
                  [{:id "3", :name "Charlie", :score {:level 2}, :age 10}
                   {:id "2", :name "Hannah", :score {:level 3}, :age 33}
                   {:id "1", :name "John", :score {:level 4}, :age 34}
                   {:id "4", :name "Leo", :age 19, :score {:level 1}}]))

         (t/is (= (sut/get-sort db [nil :persons :level-desc])
                  [{:id "1", :name "John", :score {:level 4}, :age 34}
                   {:id "2", :name "Hannah", :score {:level 3}, :age 33}
                   {:id "3", :name "Charlie", :score {:level 2}, :age 10}
                   {:id "4", :name "Leo", :age 19, :score {:level 1}}]))

         (t/is (= (sut/get-sort db [nil :persons :age-asc])
                  [{:id "3", :name "Charlie", :score {:level 2}, :age 10}
                   {:id "4", :name "Leo", :age 19, :score {:level 1}}
                   {:id "2", :name "Hannah", :score {:level 3}, :age 33}
                   {:id "1", :name "John", :score {:level 4}, :age 34}]))))

(t/deftest op-get-and-set
  (let [coll [{:id "1" :name "John" :score {:level 4} :genre :human :age 34}
              {:id "2" :name "Hannah" :score {:level 3} :genre :human :age 33}
              {:id "3" :name "Charlie" :score {:level 2} :genre :pet :age 10}
              {:id "4" :name "Leo" :score {:level 1} :genre :pet :age 19}
              {:id "5" :name "Peps" :score {:level 0} :genre :human :age 3}]
        db   (-> {}
                 (sut/add-collection [nil :persons :id coll])
                 (sut/add-op [nil {:coll-name :persons
                                   :op-name   :humans-by-level
                                   :xf        (filter (comp #{:human} :genre))
                                   :psort     (sut/psort-by-desc (comp :level :score))}
                              coll])
                 (sut/add-op [nil {:coll-name :persons
                                   :op-name   :pets-starting-with-c
                                   :xf        (filter #(str/starts-with? (:name %) "C"))}
                              coll])
                 (sut/add-op [nil {:coll-name :persons
                                   :op-name   :kids
                                   :xf        (filter #(and (> 14 (:age %))
                                                            (= :human (:genre %))))}
                              coll]))]

    (t/is (= coll
             (sut/get-op db [nil {:coll-name :persons :op-name :default}])))

    (t/is (= (sut/get-op db [nil {:coll-name :persons :op-name :humans-by-level}])
             [{:id "1", :name "John", :score {:level 4}, :genre :human, :age 34}
              {:id "2", :name "Hannah", :score {:level 3}, :genre :human, :age 33}
              {:id "5", :name "Peps", :score {:level 0}, :genre :human, :age 3}]))

    (t/is (= (sut/get-op db [nil {:coll-name :persons :op-name :pets-starting-with-c}])
             [{:id "3", :name "Charlie", :score {:level 2}, :genre :pet, :age 10}]))

    (t/is (= (sut/get-op db [nil {:coll-name :persons :op-name :kids}])
             [{:id "5", :name "Peps", :score {:level 0}, :genre :human, :age 3}]))
    db
    )
  )


(t/deftest op-test
       (let [coll          [{:id "1" :name "John" :score {:level 4} :genre :human :age 34}
                            {:id "2" :name "Hannah" :score {:level 3} :genre :human :age 33}
                            {:id "3" :name "Charlie" :score {:level 2} :genre :pet :age 10}
                            {:id "4" :name "Leo" :score {:level 1} :genre :pet :age 19}
                            {:id "5" :name "Peps" :score {:level 0} :genre :human :age 3}]
             ident         (fn [x] (:id x))
             xf            (filter (comp #{:human} :genre))
             psort         (partial sort-by :age #(compare %2 %1))
             psort-age-asc (partial sort-by :age)
             pets          (filter (comp #{:pet} :genre))]
         (t/is (= (sut/op {:xf xf :psort psort :ident ident} coll)
                 ["1" "2" "5"]))
         (t/is (= (sut/op {:ident :name} coll)
                  ["John" "Hannah" "Charlie" "Leo" "Peps"]))
         (t/is (= (sut/op {:psort psort-age-asc} coll)
                  ["5" "3" "4" "2" "1"]))
         (t/is (= (sut/op {:xf pets} coll)
                  ["3" "4"]))
         (t/is (= (sut/op {:psort (sut/psort-by-desc :age)} coll)
                  ["1" "2" "4" "3" "5"]))
         )
       )
