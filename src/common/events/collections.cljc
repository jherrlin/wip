(ns common.events.collections
  (:require
   [clojure.spec.alpha :as s]))


(s/def ::ident (s/or :k keyword? :f fn?))
(defn normalize-collection
  "Normalize vector into a map where key is gotten by `ident`."
  [ident xs]
  {:pre [(s/valid? ::ident ident)]}
  (reduce (fn [m x] (assoc m (ident x) x)) {} xs))

(defn realise-idents [ms xs]
  (->> xs
       (map #(get ms %))
       (remove nil?)
       (into [])))

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

(defn add-collection [db [_ {:keys [coll-name ident coll]}]]
  (-> db
      (assoc-in (flatten [:collections coll-name :values]) (normalize-collection ident coll))
      (assoc-in (flatten [:collections coll-name :organized :default]) (mapv ident coll))
      (assoc-in (flatten [:collections coll-name :fns       :default]) (partial mapv ident))))

(defn get-op
  [db [_ {:keys [coll-name op-name]
          :or {op-name :default}}]]
  (let [m      (get-in db (flatten [:collections coll-name :values]))
        idents (get-in db (flatten [:collections coll-name :organized op-name]))]
    (realise-idents m idents)))

(defn update-in-coll [db [_ coll-name attr value]]
  (assoc-in db (flatten [:collections coll-name :values attr]) value))

(defn remove-in-coll [db [_ coll-name attr]]
  (update-in db (flatten [:collections coll-name :values]) dissoc attr))

(defn get-in-coll [db [_ coll-name attr]]
  (get-in db (flatten [:collections coll-name :values attr])))

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
    :e add-collection
    :s get-op}

   {:n :update-in-collection
    :e update-in-coll}

   {:n :remove-in-collection
    :e remove-in-coll}

   {:n :get-in-collection
    :s get-in-coll}

   {:n :collection-update
    :e update-organized}])
