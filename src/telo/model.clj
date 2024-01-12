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
                  
                  {:db/ident :batch/formula
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :batch/doses
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/doc "number of days or doses in a batch"}
                  {:db/ident :batch/date 
                   :db/valueType :db.type/instant
                   :db/cardinality :db.cardinality/one}
                  
                  {:db/ident :batch-item/batch
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :batch-item/nutrient
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :batch-item/weight
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/doc "total weight of all doses in milligrams"}
                  {:db/ident :batch-item/complete?
                   :db/valueType :db.type/boolean
                   :db/cardinality :db.cardinality/one
                   :db/doc "a batch item is complete when it is weighed out and added to the batch"}
                  
                  {:db/ident :user/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :user/email
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :user/password
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :user/permissions
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many}
                  
                  {:db/ident :permission/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}


                  {:db/ident :test/some-url
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/many}
                  {:db/ident :test/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}])
;; retract :db/ident :formula-item/formula-name

(d/transact conn {:tx-data telo-schema})

;; see https://clojuredocs.org/clojure.core/juxt for a smaple of using juxt with sort-by to sort by multiple keys, 
;; as we have done below

(defn find-all-nutrients
  "Returns a sorted vector of maps that contain all nutrients currently in the database as of 'now'. 
   The addition of the :keys list of keys in the datalog query syntax transforms the result into a vector of maps,
   which makes the result convenient to use in an html view."
  []
  (sort-by (juxt :c-sort-order :name) (d/q '[:find ?e ?name ?grams-in-stock ?purchase-url ?note ?category-name ?c-sort-order
                                             :keys eid name grams-in-stock purchase-url note category-name c-sort-order
                                             :where [?e :nutrient/name ?name]
                                             [?e :nutrient/grams-in-stock ?grams-in-stock]
                                             [?e :nutrient/purchase-url ?purchase-url]
                                             [?e :nutrient/note ?note]
                                             [?e :nutrient/category ?c]
                                             [?c :category/name ?category-name]
                                             [?c :category/sort-order ?c-sort-order]]
                                           (d/db conn))))

(defn group-nutrients-by-category
  []
  (group-by :category-name (find-all-nutrients)))

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
  (sort-by (juxt :sort-order :nutrient-name)
           (d/q '[:find ?e ?nutrient-name ?milligrams-per-day ?sort-order ?neid
                  :keys eid nutrient-name milligrams-per-day sort-order neid
                  :in $ ?formula-id
                  :where [?e :formula-item/formula ?formula-id]
                  [?e :formula-item/milligrams-per-day ?milligrams-per-day]
                  [?e :formula-item/nutrient ?neid]
                  [?neid :nutrient/name ?nutrient-name]
                  [?neid :nutrient/category ?ceid]
                  [?ceid :category/sort-order ?sort-order]]
                (d/db conn) formula-id)))

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
  ;; The above pull pattern will return a map similar to the following
  
  {:db/id 4611681620380876949
   :formula-item/nutrient
   {:db/id 4611681620380876878, :nutrient/name "Vitamin A", :nutrient/category #:category{:name "Vitamins"}}
   :formula-item/milligrams-per-day 55
   :formula-item/formula {:db/id 4611681620380876948, :formula/name "Complete Daily Multivitamin"}}
  
  ;; so the values needed can be pulled out using (get-in ...)
  (def formula-item (pull-formula-item 4611681620380876949))
  
  (get-in formula-item [:formula-item/formula :db/id])
  
  ;; Note that
  #:category{:name "Vitamins"} ;; is clojure shorthand for {:category/name "Vitamins"}
  
  ;; Hence 
  (get-in formula-item [:formula-item/nutrient :nutrient/category :category/name]) ;; will return "Vitamins"
  )

(defn find-all-batches
  "Returns a sorted vector of maps that contain all batches with most recent first. sort-by 
   accepts an optional third argument, a custom comparator function. (compare ...) returns -1, 0 or 1
   and works in a type independent way, hence we use it here so it functions with datomic's instant type, date/time values. 
   Numeric functions like - and < were tried to reverse the sort but threw a type error."
  []
  (sort-by :date #(compare %2 %1) (d/q '[:find ?e ?formula-name ?doses ?date
                                         :keys eid formula-name doses date
                                         :where [?e :batch/formula ?feid]
                                         [?feid :formula/name ?formula-name]
                                         [?e :batch/doses ?doses]
                                         [?e :batch/date ?date]]
                                       (d/db conn))))

(defn- save-new-batch-items
  [batch-eid formula-eid doses]
  (for [fi (find-all-formula-items formula-eid)]
    (d/transact conn {:tx-data [{:batch-item/batch batch-eid
                                 :batch-item/nutrient (:neid fi)
                                 :batch-item/weight (* (:milligrams-per-day fi) doses)
                                 :batch-item/complete? false}]})))

(defn save-new-batch
  [m]
  (let [batch-eid
        (get (-> conn
                 (d/transact {:tx-data [{:db/id "new-batch"
                                         :batch/formula (:formula m)
                                         :batch/doses (:doses m)
                                         :batch/date (java.util.Date.)}]})
                 :tempids) "new-batch")]
    (save-new-batch-items batch-eid (:formula m) (:doses m))))

(defn find-batch
  [eid]
  (d/q '[:find ?e ?formula-name ?doses ?date (sum ?weight) (count ?bi)
         :keys e formula-name doses date total-weight count
         :in $ ?e
         :where [?e :batch/formula ?fe]
         [?fe :formula/name ?formula-name]
         [?e :batch/doses ?doses]
         [?e :batch/date ?date]
         [?bi :batch-item/batch ?e]
         [?bi :batch-item/weight ?weight]]
       (d/db conn) eid))

(defn find-batch-items
  [beid]
  (sort-by (juxt :c-sort-order :name) (d/q '[:find ?eid ?name ?c-sort-order ?weight ?complete
         :keys eid name c-sort-order weight complete
         :in $ ?beid
         :where [?eid :batch-item/batch ?beid]
         [?eid :batch-item/nutrient ?neid]
         [?neid :nutrient/name ?name]
         [?neid :nutrient/category ?ceid]
         [?ceid :category/sort-order ?c-sort-order]
         [?eid :batch-item/weight ?weight]
         [?eid :batch-item/complete? ?complete]] (d/db conn) beid)))

(defn pull-batch
  [eid]
  (d/pull (d/db conn) '[:db/id
                        {:batch/formula [:db/id :formula/name]}
                        :batch/doses
                        :batch/date
                        {:batch-item/_batch [:db/id
                                             {:batch-item/nutrient [:nutrient/name
                                                                    {:nutrient/category [:category/sort-order]}]}
                                             :batch-item/weight
                                             :batch-item/complete?]}] eid))
  
(defn pull-batch-item
  [eid]
  (d/pull (d/db conn) '[:db/id
                        {:batch-item/nutrient [:db/id :nutrient/name :nutrient/grams-in-stock]}
                        :batch-item/weight
                        :batch-item/complete?
                        :batch-item/batch [:db/id]] eid))

(defn save-batch-item
  [m]
  (d/transact conn {:tx-data [{:db/id (:id m)
                               :batch-item/weight (:weight m)
                               :batch-item/complete? (:complete? m)}]}))

(defn update-nutrient-weight
  [m]
  (d/transact conn {:tx-data [{:db/id (:nid m) 
                               :nutrient/grams-in-stock (:new-grams-in-stock m)
                               }]}))

(defn subtract-batch-item-from-stock
  "Returns remaining stock of a nutrient in grams, 
   or 0 if calculated value is a negative number "
  [bi] 
  (let [new-grams-in-stock (- (get-in bi [:batch-item/nutrient :nutrient/grams-in-stock])
     (Math/round (* (:batch-item/weight bi) 0.001)))]
    (if (> new-grams-in-stock 0) new-grams-in-stock 0)))

(comment

  (- 40 5)

  (def bi (pull-batch-item 87960930222289))
  (:batch-item/complete? bi)

  (def mn (hash-map :nid (get-in bi [:batch-item/nutrient :db/id])
                   :new-grams-in-stock (subtract-batch-item-from-stock bi)))
  
  mn

  (update-nutrient-weight mn)

  (def ngis (subtract-batch-item-from-stock bi))
  ngis

  (def nutrient-id (get-in bi [:batch-item/nutrient :db/id]))

  nutrient-id
  
  (def newbi (assoc bi :new-grams-in-stock ngis))

  newbi

  (-
   (get-in bi [:batch-item/nutrient :nutrient/grams-in-stock])
   (Math/round (* (:batch-item/weight bi) 0.001)))
  

  (pull-batch-item 87960930222289)

  (def batch (pull-batch 96757023244496))

  (get-in batch [:batch/formula :formula/name])

  (get-in batch [:batch/doses])

  (def batch-items (find-batch-items 96757023244496))

  (apply + (map :weight batch-items))
  ()

  (find-batch 96757023244496)

  (group-nutrients-by-category)

  (save-new-batch-items 96757023244496 4611681620380876948 90)

  (def abatch (pull-batch 96757023244496))

  (t/format "MM/dd" (.toInstant #inst "2020-09-26T23:08:27.619-00:00"))
  (:batch/date abatch)

  (t/instant? (t/zoned-date-time? #inst "2020-09-26T23:08:27.619-00:00"))

  (t/format "yyyy.MM.dd" (t/zoned-date-time 2015 9 28))

  (t/format "dd.MM.yyyy" (t/zoned-date-time (:batch/date abatch) (t/zone-id "UTC")))

  (def sf (java.text.SimpleDateFormat. "dd.MM.yyyy"))
  (.format sf (:batch/date abatch))



  (def new-batch {:formula 4611681620380876948
                  :doses 90})

  (save-new-batch new-batch)

  (-> new-batch save-new-batch :tempids)
  (get {"new-batch" 83562883711177} "new-batch")

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

  (d/q '[:find ?e
         :keys eid
         :where [?e :batch-item/batch 96757023244496]]
       (d/db conn))
  ) 