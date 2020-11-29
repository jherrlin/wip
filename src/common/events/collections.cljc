(ns common.events.collections
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))


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
  (->> xs
       (map #(get ms %))
       (remove nil?)
       (into [])))

(defn add-collection [db [_ collection-name ident xs]]
  (-> db
      (assoc-in (flatten [:collections collection-name :sorts :default]) (mapv ident xs))
      (assoc-in (flatten [:collections collection-name :values]) (normalize-collection ident xs))
      (assoc-in (flatten [:collections collection-name :organized :default]) (mapv ident xs))
      (assoc-in (flatten [:collections collection-name :fns       :default]) (partial mapv ident))))

(defn add-sort [db [_ collection-name sort-name sort-fn]]
  (let [xs (vals (get-in db [:collections collection-name :m]))]
    (assoc-in db (flatten [:collections collection-name :sorts sort-name]) (sort-fn xs))))

(defn get-sort [db [_ collection-name sort-name]]
  (let [m      (get-in db (flatten [:collections collection-name :m]))
        idents (get-in db (flatten [:collections collection-name :sorts sort-name]))]
    (realise-sort
     m
     idents)))

(defn op
  "Can run a collection through a transducer `xf` and a `psort` function.
  Return a collection of entity `ident`s.

  - `ident`  The identifier of each entity in the collection. Default `:id`.
  - `xf`     Transducer, optional.
  - `psort`  Partial sorting function, optional.
  - `coll`   Collection to operate on"
  [{:keys [xf psort ident]
    :or   {ident :id}}
   coll]
  {:pre [(s/valid? coll? coll)]}
  (cond->> coll
    xf      (into [] xf)
    psort   (psort)
    :always (mapv ident)))

(defn psort-by
  "Partial sort by."
  [attribute & [compare-fn]]
  (partial sort-by attribute (or compare-fn #(compare %1 %2))))

(defn psort-by-asc
  "Partial sort by with ascending order."
  [attribute]
  (psort-by attribute #(compare %1 %2)))

(defn psort-by-desc
  "Partial sort by with descending order."
  [attribute]
  (psort-by attribute #(compare %2 %1)))

(defn add-op
  [db [_ {:keys [coll-name op-name xf psort ident]
          :or {op-name :default}
          :as m}
       coll]]
  {:pre [(s/valid? (complement nil?) coll-name)
         (s/valid? (complement nil?) op-name)]}
  (-> db
      (assoc-in (flatten [:collections coll-name :organized op-name]) (op m coll))
      (assoc-in (flatten [:collections coll-name :fns       op-name]) (partial op m))))

(defn get-op
  [db [_ {:keys [coll-name op-name]
          :or {op-name :default}}]]
  (let [m      (get-in db (flatten [:collections coll-name :values]))
        idents (get-in db (flatten [:collections coll-name :organized op-name]))]
    (realise-sort m idents)))

(defn update-in-coll [db [_ coll-name attr value]]
  (assoc-in db (flatten [:collections coll-name :values attr]) value))

(defn remove-in-coll [db [_ coll-name attr]]
  (update-in db (flatten [:collections coll-name :values]) dissoc attr))

(defn run-fns-on-xs [fns xs]
  (reduce-kv
   (fn [m k v]
     (assoc m k (v xs)))
   {}
   fns))

(defn update-organized*
  [{:keys [values fns] :as m}]
  (let [xs      (vals values)
        updated (run-fns-on-xs fns xs)]
    (assoc m :organized updated)))

(defn update-organized
  [db [_ coll-name]]
  (let [xs (get-in db (flatten [:collections coll-name]))]
    (assoc-in db (flatten [:collections coll-name]) (update-organized* xs))))

(def collection-events
  [{:n :collection
    :e add-collection}
   {:n :sort
    :e add-sort
    :s get-sort}])

(let [coll [{:id 1
             :name "A Movie 1"}
            {:id 2
             :name "A Movie 2"}
            {:id 3
             :name "A Movie 3"}]]
  (-> {}
      (add-collection [nil [:movie-project "A"] :id coll])
      (remove-in-coll [nil [:movie-project "A"] 3])
      ))
