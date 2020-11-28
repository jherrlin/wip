(ns common.events.collections
  (:require
   [clojure.spec.alpha :as s]))


(s/def ::ident (s/or :k keyword? :f fn?))
(defn normalize-collection
  "Normalize vector into a map where key is gotten by `ident`."
  [ident xs]
  {:pre [(s/valid? ::ident ident)]}
  (reduce (fn [m x] (assoc m (ident x) x)) {} xs))

(s/def ::attribute-to-sort (s/or :k keyword? :f fn?))
(s/def ::compare-fn ::attribute-to-sort)
(s/def ::ident ::attribute-to-sort)
(s/def ::ms (s/coll-of map?))
(defn sort-and-reduce-to-ident
  ([attribute-to-sort ident ms]
   (sort-and-reduce-to-ident attribute-to-sort #(compare %1 %2) ident ms))
  ([attribute-to-sort compare-fn ident ms]
   {:pre [(s/valid? ::attribute-to-sort attribute-to-sort)
          (s/valid? ::compare-fn compare-fn)
          (s/valid? ::ident ident)
          (s/valid? ::ms ms)]}
   (->> ms (sort-by attribute-to-sort compare-fn) (mapv ident))))

(defn asc [sort-attribute ident xs]
  (sort-and-reduce-to-ident sort-attribute #(compare %1 %2) ident xs))

(defn desc [sort-attribute ident xs]
  (sort-and-reduce-to-ident sort-attribute #(compare %2 %1) ident xs))

(defn realise-sort [ms xs]
  (mapv #(get ms %) xs))

(defn add-collection [db [_ collection-name ident xs]]
  (assoc-in db [:collections collection-name :m] (normalize-collection ident xs)))

(defn add-sort [db [_ collection-name sort-name sort-fn]]
  (let [xs (vals (get-in db [:collections collection-name :m]))]
    (assoc-in db [:collections collection-name :sorts sort-name] (sort-fn xs))))

(defn get-sort [db [_ collection-name sort-name]]
  (let [m      (get-in db [:collections collection-name :m])
        idents (get-in db [:collections collection-name :sorts sort-name])]
    (realise-sort
     m
     idents)))

(def events
  {:collection
   {:n :collection
    :e add-collection}
   :sort
   {:n :sort
    :e add-sort
    :s get-sort}})

#?(:clj
   (do
     (require '[clojure.test :as t])
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
                    (add-collection [nil :persons :id col])
                    (add-sort       [nil :persons :name-asc (partial asc :name :id)])
                    (add-sort       [nil :persons :name-desc (partial desc :name :id)])
                    (add-sort       [nil :persons :level-asc (partial asc (comp :level :score) :id)])
                    (add-sort       [nil :persons :level-desc (partial desc (comp :level :score) :id)])
                    (add-sort       [nil :persons :age-asc (partial asc :age :id)])
                    (add-sort       [nil :persons :age-desc (partial desc :age :id)]))]

         (t/is (= (get-sort db [nil :persons :name-asc])
                  [{:id "3", :name "Charlie", :score {:level 2}, :age 10}
                   {:id "2", :name "Hannah", :score {:level 3}, :age 33}
                   {:id "1", :name "John", :score {:level 4}, :age 34}
                   {:id "4", :name "Leo", :age 19, :score {:level 1}}]))

         (t/is (= (get-sort db [nil :persons :level-desc])
                  [{:id "1", :name "John", :score {:level 4}, :age 34}
                   {:id "2", :name "Hannah", :score {:level 3}, :age 33}
                   {:id "3", :name "Charlie", :score {:level 2}, :age 10}
                   {:id "4", :name "Leo", :age 19, :score {:level 1}}]))

         (t/is (= (get-sort db [nil :persons :age-asc])
                  [{:id "3", :name "Charlie", :score {:level 2}, :age 10}
                   {:id "4", :name "Leo", :age 19, :score {:level 1}}
                   {:id "2", :name "Hannah", :score {:level 3}, :age 33}
                   {:id "1", :name "John", :score {:level 4}, :age 34}]))
         )
       )
     (t/run-tests)
     ))
