(ns telo.model
  (:require [datomic.client.api :as d])
  
)
(def config {:server-type :dev-local
             :system "dev"})

(def client (d/client config))

(d/create-database client {:db-name "telogenix"})

(def conn (d/connect client {:db-name "telogenix"}))


(def telo-schema [{:db/ident :nutrient/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "A nutrient / supplement name"}
                  {:db/ident :nutrient/grams-in-stock
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/doc "Total grams in stock of this nutrient"}
                  {:db/ident :nutrient/purchase-url
                   :db/valueType :db.type/uri
                   :db/cardinality :db.cardinality/one
                   :db/doc "Link to purchase this nutrient online"}
                  {:db/ident :nutrient/note
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "Notes on this nutrient"}
                  
                  
                  ])

(d/transact conn {:tx-data telo-schema})



(comment
  )