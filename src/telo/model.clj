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

(defn find-all-nutrients []
  (sort-by :name (d/q '[:find ?e ?name ?grams-in-stock ?purchase-url ?note
         :keys eid name grams-in-stock purchase-url note
         :where [?e :nutrient/name ?name]
         [?e :nutrient/grams-in-stock ?grams-in-stock]
         [?e :nutrient/purchase-url ?purchase-url]
         [?e :nutrient/note ?note]
         ]
       (d/db conn))))

(defn find-nutrient [eid]
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

(defn pull-nutrient [eid]
  (d/pull (d/db conn) '[:db/id 
                        :nutrient/name 
                        :nutrient/grams-in-stock 
                        :nutrient/purchase-url 
                        :nutrient/note 
                        :nutrient/category]
  eid))

(defn save-nutrient [m]
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
  
  (sorted-map )
  
  (d/pull (d/db conn) '[:db/id 
                        :nutrient/name 
                        :nutrient/grams-in-stock 
                        :nutrient/purchase-url 
                        :nutrient/note 
                        :nutrient/category] 4611681620380876878)
  
  (((pull-nutrient 4611681620380876878) :nutrient/category) :db/id)
  
  
  
  
  (find-all-nutrients)
  
  )