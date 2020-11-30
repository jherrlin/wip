(ns common.events.collections2-test
  (:require [common.events.collections2 :as sut]
            [clojure.test :as t]))

;; (remove-ns 'common.events.collections2-test)

(def coll [{:id "1" :name "John" :score {:level 4} :genre :human :age 34}
           {:id "2" :name "Hannah" :score {:level 3} :genre :human :age 33}
           {:id "3" :name "Charlie" :score {:level 2} :genre :pet :age 10}
           {:id "4" :name "Leo" :score {:level 1} :genre :pet :age 19}
           {:id "5" :name "Peps" :score {:level 0} :genre :human :age 3}])

(t/deftest transformer-test
  (t/testing "transformer"
    (t/is (= (sut/transformer {:xf (filter (comp #{:human} :genre))
                               :psort (partial sort-by :age #(compare %1 %2))}
                              :id
                              coll)
             ["5" "2" "1"])))

  (t/testing "transformer with partials"
    (let [p1-transformer (sut/transformer {:xf (filter (comp #{:human} :genre))})
          p2-transformer (partial p1-transformer :id)]
      (t/is (= (p2-transformer coll)
               ["1" "2" "5"])))))

(t/deftest add-entities-flow
  (t/testing "Add entities and transformation and run all of them and check results."
    (let [humans-t (sut/transformer {:xf (filter (comp #{:human} :genre))})
          pets-t   (sut/transformer {:xf (filter (comp #{:pet} :genre))})
          db (-> {}
                 (sut/add-entities         {:location [:family] :ident :id :entities coll})
                 (sut/add-transformer      {:location [:family] :selector :humans :transformer humans-t})
                 (sut/add-transformer      {:location [:family] :selector :pets :transformer pets-t})
                 (sut/run-all-transformers {:location [:family]}))]
      (t/is (= (sut/get-entities db {:location [:family] :selector :humans})
               [{:id "1", :name "John", :score {:level 4}, :genre :human, :age 34}
                {:id "2", :name "Hannah", :score {:level 3}, :genre :human, :age 33}
                {:id "5", :name "Peps", :score {:level 0}, :genre :human, :age 3}]))

      (t/is (= (sut/get-entities db {:location [:family] :selector :pets})
               [{:id "3", :name "Charlie", :score {:level 2}, :genre :pet, :age 10}
                {:id "4", :name "Leo", :score {:level 1}, :genre :pet, :age 19}]))
      db)
    ))

(t/deftest add-entities-and-transformation
  (t/testing "Add entities and transformation run it."
    (let [transformer (sut/transformer {:xf (filter (comp #{:pet} :genre))})
          base {:location [:family] :selector :humans}
          db (-> {}
                 (sut/add-entities      (merge base {:ident :id :entities coll}))
                 (sut/add-transformer   (merge base {:transformer transformer}))
                 (sut/run-transformer   (merge base)))]
      (t/is (= (sut/get-sequence db {:location [:family] :selector :humans})
               ["3" "4"]))
      db)
    ))

(t/deftest add-sequence
  (let [db (-> {}
               (sut/add-entities         {:location [:family] :ident :id :entities coll})
               (sut/add-sequence         {:location [:family] :selector :n2 :sequence ["2"]}))]
    (t/is (= (sut/get-entities db {:location [:family] :selector :n2})
             [{:id "2", :name "Hannah", :score {:level 3}, :genre :human, :age 33}]))
    db)
  )

(t/deftest add-entity-and-run-transformations
  (let [humans-t (sut/transformer {:xf (filter (comp #{:human} :genre))})
        new-p {:id "6" :name "Love" :score {:level -1} :genre :human :age 0}
        base {:location [:family]}
        db (-> {}
               (sut/add-entities         (merge base {:ident :id :entities coll}))
               (sut/add-entity           (merge base {:entity new-p}))
               (sut/add-transformer      (merge base {:selector :humans :transformer humans-t}))
               (sut/run-transformer      (merge base {:selector :humans})))]
    (t/is (= (sut/get-sequence db (merge base {:selector :humans}))
             ["1" "2" "5" "6"]))
    db)
  )

(t/deftest remove-entity
  (let [base {:location [:family]}
        humans-t (sut/transformer {:xf (filter (comp #{:human} :genre))})
        db (-> {}
               (sut/add-entities         (merge base {:ident :id :entities coll}))
               (sut/add-transformer      (merge base {:selector :humans :transformer humans-t}))
               (sut/run-transformer      (merge base {:selector :humans}))
               (sut/remove-entity        (merge base {:entity-ident "1"}))
               (sut/remove-entity        (merge base {:entity-ident "5"}))
               (sut/run-transformer      (merge base {:selector :humans})))]
    (t/is (= (sut/get-sequence db (merge base {:selector :humans}))
             ["2"]))
    db)
  )

(t/deftest update-in-entity-and-get-it
  (let [base {:location [:family]}
        humans-t (sut/transformer {:xf (filter (comp #{:human} :genre))})
        db (-> {}
               (sut/add-entities         (merge base {:ident :id :entities coll}))
               (sut/update-in-entity     (merge base {:entity-path ["1" :name] :value "JOHN!"}))
               (sut/add-transformer      (merge base {:selector :humans :transformer humans-t}))
               (sut/run-transformer      (merge base {:selector :humans}))
               (sut/run-transformer      (merge base {:selector :humans})))]
    (t/is (= (sut/get-entity db (merge base {:entity-ident "1"}))
             {:id "1", :name "JOHN!", :score {:level 4}, :genre :human, :age 34}))
    db)
  )

(t/deftest get-ident
  (let [base  {:location [:family]}
        ident (fn [x] (:id x))
        db    (sut/add-entities {} (merge base {:ident ident :entities coll}))]
    (t/is (= (sut/get-ident db base)
             ident))
    db)
  )
