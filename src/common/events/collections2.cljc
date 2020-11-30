;; Main fns:
;; `add-entities`          [db {:keys [location entities ident] :as m}]
;; `add-entity`            [db {:keys [location entity]}]
;; `get-entity`            [db {:keys [location entity-ident]}]
;; `remove-entity`         [db {:keys [location entity-ident]}]
;; `run-all-transformers`  [db {:keys [location]}]
;; `run-transformer`       [db {:keys [location selector]}]
;;
;; Example datastructure:
;; {:collections
;;  {:family
;;   {:entities
;;    {"1" {:id "1", :name "John", :score {:level 4}, :genre :human, :age 34},
;;     "2" {:id "2", :name "Hannah", :score {:level 3}, :genre :human, :age 33},
;;     "3" {:id "3", :name "Charlie", :score {:level 2}, :genre :pet, :age 10},
;;     "4" {:id "4", :name "Leo", :score {:level 1}, :genre :pet, :age 19},
;;     "5" {:id "5", :name "Peps", :score {:level 0}, :genre :human, :age 3}},
;;    :ident :id,
;;    :transformers
;;    {:humans #function[clojure.core/partial/fn--5839],
;;     :pets #function[clojure.core/partial/fn--5839]},
;;    :sequences {:humans ["1" "2" "5"],
;;                :pets ["3" "4"]}}}}
;;
(ns common.events.collections2
  (:require
   [clojure.spec.alpha :as s]))


(defn transformer
  "Can run a collection through a transducer `xf` and a `psort` function.
  Return a collection of entity `ident`s.

  - `xf`     Transducer, optional.
  - `psort`  Partial sorting function, optional.
  - `ident`  The identifier of each entity in the collection. Default `:id`.
  - `coll`   Collection to operate on"
  ([m]
   (partial transformer m))
  ([m ident]
   (partial transformer m ident))
  ([{:keys [xf psort]} ident coll]
   (cond->> coll
     xf      (into [] xf)
     psort   (psort)
     :always (mapv ident))))

(s/def ::ident (s/or :k keyword? :f fn?))
(defn normalize-collection
  "Normalize vector into a map where key is gotten by `ident`."
  [ident xs]
  {:pre [(s/valid? ::ident ident)]}
  (reduce (fn [m x] (assoc m (ident x) x)) {} xs))

(defn realise-idents [m xs]
  {:pre [(s/valid? coll? xs)
         (s/valid? map? m)]}
  (->> xs
       (map #(get m %))
       (remove nil?)
       (into [])))


;; Transformers
;; Applies transducer and sorting fn on collection. Both of them are optional.
;; Returns a collection with entity idents.
(defn add-transformer [db {:keys [location selector transformer]
                           :or {selector :all-idents}}]
  (assoc-in db (flatten [:collections location :transformers selector]) transformer))

(defn get-transformer [db {:keys [location selector]}]
  (get-in db (flatten [:collections location :transformers selector])))

(defn remove-transformer [db {:keys [location selector]}]
  (update-in db (flatten [:collections location :transformers]) dissoc selector))

(defn run-all-transformers [db {:keys [location]}]
  (let [{:keys [entities transformers ident]}
        (get-in db (flatten [:collections location]))
        xs (vals entities)]
    (assoc-in db
              (flatten [:collections location :sequences])
              (reduce-kv
               (fn [m k v]
                 (assoc m k (v ident xs)))
               {}
               transformers))))

(defn run-transformer [db {:keys [location selector]}]
  (let [{:keys [entities transformers ident]}
        (get-in db (flatten [:collections location]))
        xs (vals entities)
        transformer (get transformers selector)]
    (assoc-in db
              (flatten [:collections location :sequences selector])
              (transformer ident xs))))


;; Ident
;; Used to identify the unique attribute in a entity.
;; Ident are the items in the sequences and when asked for they are lookup in
;; the entities map.
(defn add-ident [db {:keys [location ident]}]
  (assoc-in db (flatten [:collections location :ident]) ident))

(defn get-ident [db {:keys [location]}]
  (get-in db (flatten [:collections location :ident])))

(defn remove-ident [db {:keys [location]}]
  (update-in db (flatten [:collections location]) assoc :ident nil))


;; Sequences
;; Represent entities by ident in a specific order
(defn add-sequence [db {:keys [location selector sequence]}]
  (assoc-in db (flatten [:collections location :sequences selector]) sequence))

(defn get-sequence [db {:keys [location selector]}]
  (get-in db (flatten [:collections location :sequences selector])))

(defn remove-sequence [db {:keys [location selector]}]
  (update-in db (flatten [:collections location :sequences]) assoc selector nil))


;; Entities
;; Hashmap that contains normalized entities. The key is the ident of the entity
;; and the value is the entity
(defn add-entities [db {:keys [location entities ident] :as m}]
  (-> db
      (add-ident m)
      (assoc-in (flatten [:collections location :entities]) (normalize-collection ident entities))
      (assoc-in (flatten [:collections location :transformers :all-idents]) (transformer {}))
      (run-all-transformers m)))

(defn get-entities [db {:keys [location selector]}]
  (let [{:keys [entities sequences]}
        (get-in db (flatten [:collections location]))]
    (realise-idents entities (get sequences selector))))

(defn get-entity [db {:keys [location entity-ident]}]
  (get-in db (flatten [:collections location :entities entity-ident])))

(defn add-entity  [db {:keys [location entity]}]
  (let [{:keys [ident]} (get-in db (flatten [:collections location]))]
    (assoc-in db (flatten [:collections location :entities (ident entity)]) entity)))

(defn remove-entity [db {:keys [location entity-ident]}]
  (update-in db (flatten [:collections location :entities]) dissoc entity-ident))

(defn update-in-entity [db {:keys [location entity-path value]}]
  {:pre [(s/valid? coll? entity-path)]}
  (update-in db (flatten [:collections location :entities]) assoc-in entity-path value))
