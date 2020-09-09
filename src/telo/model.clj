(ns telo.model
  (:require [datomic.client.api :as d])
  
)
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
                   :db/cardinality :db.cardinality/one
                   }
                  ])

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
         [?eid :nutrient/category ?category-eid]
         ]
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

(defn retract-nutrient [eid]
  (d/transact conn {:tx-data [[:db/retractEntity eid]]}))

(defn find-all-categories []
  (sort-by :sort-order (d/q '[:find ?e ?name ?sort-order
         :keys catid name sort-order 
         :where [?e :category/name ?name]
         [?e :category/sort-order ?sort-order]
         ]
       (d/db conn)))
  )



(comment

  
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
  
  )