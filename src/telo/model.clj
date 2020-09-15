(ns telo.model
  (:require [datomic.client.api :as d]))
(def config {:server-type :dev-local
             :system "dev"})

(def client (d/client config))

(d/create-database client {:db-name "telogenix"})

;; (d/delete-database client {:db-name "telogenix"})

(def conn (d/connect client {:db-name "telogenix"}))


(def telo-schema [{:db/ident :nutrient/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "A nutrient / supplement name"}
                  {:db/ident :nutrient/category
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :nutrient/grams-in-stock
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/doc "Total grams in stock of this nutrient"}
                  {:db/ident :nutrient/purchase-url
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "Link to purchase this nutrient online"}
                  {:db/ident :nutrient/note
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "Notes on this nutrient"}
                  {:db/ident :category/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity
                   :db/doc "Nutrient category"}
                  {:db/ident :category/sort-order
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :formula/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :formula-item/nutrient
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :formula-item/milligrams-per-day
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :formula-item/formula
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}


                  {:db/ident :test/some-url
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/many}
                  {:db/ident :test/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}])
;; retract :db/ident :formula-item/formula-name

(d/transact conn {:tx-data telo-schema})

(defn find-all-nutrients
  "Returns a sorted vector of maps that contain all nutrients currently in the database as of 'now'. 
   The addition of the :keys list of keys in the datalog query syntax transforms the result into a vector of maps,
   which makes the result convenient to use in an html view."
  []
  (sort-by :c-sort-order
           (sort-by :name (d/q '[:find ?e ?name ?grams-in-stock ?purchase-url ?note ?category-name ?c-sort-order
                                 :keys eid name grams-in-stock purchase-url note category-name c-sort-order
                                 :where [?e :nutrient/name ?name]
                                 [?e :nutrient/grams-in-stock ?grams-in-stock]
                                 [?e :nutrient/purchase-url ?purchase-url]
                                 [?e :nutrient/note ?note]
                                 [?e :nutrient/category ?c]
                                 [?c :category/name ?category-name]
                                 [?c :category/sort-order ?c-sort-order]]
                               (d/db conn)))))

(defn find-all-formulas
  "Returns a sorted vector of maps that contain all formula names in the database.
   A formula name identifies a collection of formula items"
  []
  (sort-by :name (d/q '[:find ?e ?name
                        :keys eid name
                        :where [?e :formula/name ?name]]
                      (d/db conn))))

(defn find-nutrient
  "Returns a vector containing a map of a specific nutrient by entity id. 
   Note that each attribute specified in the :where clause must be present as a datom in datomic, 
   or this query will return nil. If the schema evolves to add further attributes to this entity, 
   and this query is modified to add those attributes, it will no longer find entities that lack those
   attributes. The pull syntax does not have this limitation. "
  [eid]
  (d/q '[:find ?eid ?name ?grams-in-stock ?purchase-url ?note ?category-eid
         :keys eid name grams-in-stock purchase-url note category-eid
         :in $ ?eid
         :where [?eid :nutrient/name ?name]
         [?eid :nutrient/grams-in-stock ?grams-in-stock]
         [?eid :nutrient/purchase-url ?purchase-url]
         [?eid :nutrient/note ?note]
         [?eid :nutrient/category ?category-eid]]
       (d/db conn) eid))

(defn pull-nutrient
  "Returns a map of the nutrient attributes specified by entity id. If a datom is missing from an entity,
   the pull syntax will still return a result. See https://docs.datomic.com/cloud/query/query-pull.html"
  [eid]
  (d/pull (d/db conn) '[:db/id
                        :nutrient/name
                        :nutrient/grams-in-stock
                        :nutrient/purchase-url
                        :nutrient/note
                        :nutrient/category]
          eid))

(defn save-nutrient
  "Given a map of nutrient values m, this function persists the individual datoms specified to datomic.
  If the entity id specified by :db/id exists, then an upsert will be performed, which will retract any existing datoms 
  if the value of the datom has changed and insert a new one in its place, insert datoms that don't exist using
  the entity id of its siblings, and leave any datoms with unchanged values alone. 
  
  If the entity id passed in does not exist in datomic as a :db/id on a set of datoms, then datomic will create a 
  new, unique entity id to be stored as :db/id and persist the datoms specified below with that entity id."
  [m]
  (d/transact conn {:tx-data [{:db/id (:id m)
                               :nutrient/name (:name m)
                               :nutrient/grams-in-stock (:grams-in-stock m)
                               :nutrient/purchase-url (:purchase-url m)
                               :nutrient/note (:note m)
                               :nutrient/category (:category m)}]}))

(defn retract-entity [eid]
  (d/transact conn {:tx-data [[:db/retractEntity eid]]}))

(defn find-all-categories
  "Returns a sorted vector of maps that contain all nutrient categories"
  []
  (sort-by :sort-order (d/q '[:find ?e ?name ?sort-order
                              :keys catid name sort-order
                              :where [?e :category/name ?name]
                              [?e :category/sort-order ?sort-order]]
                            (d/db conn))))

(defn find-all-formulas
  "Returns a sorted vector of maps that contain all formula names in the database.
   A formula name identifies a collection of formula items"
  []
  (sort-by :name (d/q '[:find ?e ?name
                        :keys eid name
                        :where [?e :formula/name ?name]]
                      (d/db conn))))

(defn pull-formula
  [eid]
  (d/pull (d/db conn) '[:db/id
                        :formula/name] eid))

(defn save-formula
  [m]
  (d/transact conn {:tx-data [{:db/id (:id m)
                               :formula/name (:name m)}]}))

(defn save-formula-item
  [m]
  (d/transact conn {:tx-data [{:db/id (:id m)
                               :formula-item/nutrient (:nutrient m)
                               :formula-item/milligrams-per-day (:milligrams-per-day m)
                               :formula-item/formula (:formula m)}]}))

(defn find-all-formula-items
  [formula-id]
  (sort-by :sort-order
           (sort-by :nutrient-name
                    (d/q '[:find ?e ?nutrient-name ?milligrams-per-day ?sort-order ?neid
                           :keys eid nutrient-name milligrams-per-day sort-order neid
                           :in $ ?formula-id
                           :where [?e :formula-item/formula ?formula-id]
                           [?e :formula-item/milligrams-per-day ?milligrams-per-day]
                           [?e :formula-item/nutrient ?neid]
                           [?neid :nutrient/name ?nutrient-name]
                           [?neid :nutrient/category ?ceid]
                           [?ceid :category/sort-order ?sort-order]]
                         (d/db conn) formula-id))))

(defn pull-formula-item
  "Returns a nested map that is generated by traversing multiple entities 
   to obtain information about a formula item. :formula-item/nutrient is a ref type that returns
   an entity id, which will match the entity id of a nutrient entity, which has a :nutrient/name
   and a :nutrient/category. :nutrient/category is also a ref type, and we traverse directly 
   to the :category/name in the pull pattern below."
  [eid]
  (d/pull (d/db conn) '[:db/id
                        {:formula-item/nutrient [:db/id
                                                 :nutrient/name
                                                 {:nutrient/category [:category/name]}]}
                        :formula-item/milligrams-per-day
                        {:formula-item/formula [:db/id
                                                :formula/name]}] eid))

(comment 
  The above pull pattern will return a map similar to the following:
  
  {:db/id 4611681620380876949
   :formula-item/nutrient
   {:db/id 4611681620380876878, :nutrient/name "Vitamin A", :nutrient/category #:category{:name "Vitamins"}}
   :formula-item/milligrams-per-day 55
   :formula-item/formula {:db/id 4611681620380876948, :formula/name "Complete Daily Multivitamin"}}
  
  so the values needed can be pulled out using (get-in ...)
  
  Note that
  #:category{:name "Vitamins"} is clojure shorthand for {:category/name "Vitamins"}
  
  Hence (get-in formula-item [:formula-item/nutrient :nutrient/category :category/name]) would return "Vitamins"
  )


(comment

  (pull-formula-item 4611681620380876949)

  (find-all-formula-items 4611681620380876948)

  (find-all-formulas)

  (sort-by :sort-order (d/q '[:find ?e ?name ?sort-order
                              :where [?e :category/name ?name]
                              [?e :category/sort-order ?sort-order]]
                            (d/db conn)))



  (find-nutrient 4611681620380876883)

  (def cats [{:category/name "Vitamins"
              :category/sort-order 1}
             {:category/name "Minerals"
              :category/sort-order 2}
             {:category/name "Amino acids"
              :category/sort-order 3}
             {:category/name "Herbs"
              :category/sort-order 5}
             {:category/name "Plant extracts"
              :category/sort-order 4}])



  (d/transact conn {:tx-data cats})




  (def qcats [{:catid 87960930222164, :name "Vitamins", :sort-order 1}
              {:catid 96757023244374, :name "Amino acids", :sort-order 3}
              {:catid 96757023244375, :name "Herbs", :sort-order 4}
              {:catid 101155069755477, :name "Minerals", :sort-order 2}])

  (sorted-map)

  (d/pull (d/db conn) '[:db/id
                        :nutrient/name
                        :nutrient/grams-in-stock
                        :nutrient/purchase-url
                        :nutrient/note
                        :nutrient/category] "")

  (((pull-nutrient 4611681620380876878) :nutrient/category) :db/id)




  (find-all-nutrients)

  (def test-cardinality-many [{:test/name "Clojure sites"}
                              {:test/some-url "www.clojure.org"}
                              {:test/some-url "www.datomic.com"}])

  (d/transact conn {:tx-data test-cardinality-many})

  (d/q '[:find ?e ?name
         :keys eid name
         :where
         [?e :test/name ?name]]
       (d/db conn))

  (d/pull (d/db conn) '[:db/id
                        :test/name
                        :test/some-url] 101155069755537)

  (retract-nutrient 96757023244430))