(ns telo.controller
  (:require
   [ring.util.response :as resp]
   [telo.view :as view]
   [telo.model :as m]
   ))

;; helper functions
(defn- empty-to-zero 
  "Ensures that an empty numeric form field has a default value
   while returning the existing value if not empty.
   Casting is handled in a subsequent step"
  [v]
  (if (empty? v)
    "0"
    v))

(defn- empty-to-tempid
  "Ensures that an empty entity id form field has a tempid value for datomic
   while returning the existing entity id if not empty.
   Casting is handled in a subsequent step"
  [v]
  (if (empty? v)
    "-1"
    v))

(defn home [req]
  (view/home req))

(defn nutrients 
  "Controller function for the list of nutrients which injects the find-all-nutrients 
   query into a modified request map and passes it to the view"
  [req]
  (view/nutrient-list (assoc-in req [:params :q] (m/find-all-nutrients))))

(defn- wrap-nutrient-form-data 
  "If editing an existing nutrient, this function injects the form data into the request,
   otherwise it injects default form data for a new nutrient"
  [req]
  (if (get-in req [:path-params :id])
    (assoc-in req [:params :q] (m/pull-nutrient (Long/parseLong (get-in req [:path-params :id]))))
    (assoc-in req [:params :q] {:db/id -1 :nutrient/name "" :nutrient/grams-in-stock "0" :nutrient/purchase-url "" :nutrient/note ""})))

(defn- wrap-category-data 
  "Injects the nutrient categories query into a modified request map and returns it"
  [req]
  (assoc-in req [:params :qc] (m/find-all-categories)))

(defn edit-nutrient 
  "Controller function for the edit nutrients form which threads the request map
   through several functions to inject the needed data into successive modified versions 
   of the request map and then passes that into the nutrient form view"
  [req]
  (view/nutrient-form (-> req wrap-nutrient-form-data wrap-category-data)))

;; if valid, redirect to nutrient list
;; else redirect to edit-nutrient
(defn save-nutrient [req]
  (let [data (-> req
                 :params
                 (update :id #(Long/parseLong (empty-to-tempid %)))
                 (update :grams-in-stock #(Long/parseLong (empty-to-zero %)))
                 (update :category #(Long/parseLong %)))]
    (m/save-nutrient data))
  (resp/redirect "/nutrients"))

(defn delete-nutrient [req]
  (let [eid (Long/parseLong (get-in req [:path-params :id]))]
    (m/retract-entity eid))
  (resp/redirect "/nutrients"))

(defn formulas 
  "Controller function for the list of formulas which injects the find-all-formulas 
   query into a modified request map and passes it to the view"
  [req]
  (view/formula-list (assoc-in req [:params :q] (m/find-all-formulas))))

(defn delete-formula [req]
  (let [eid (Long/parseLong (get-in req [:path-params :id]))]
    (m/retract-entity eid))
  (resp/redirect "/formulas"))

(defn- wrap-formula-form-data 
  "If editing an existing formula, this function injects the form data into the request,
   otherwise it injects default form data for a new formula"
  [req]
  (if (get-in req [:path-params :id])
    (assoc-in req [:params :q] (m/pull-formula (Long/parseLong (get-in req [:path-params :id]))))
    (assoc-in req [:params :q] {:db/id -1 :formula/name ""})))

(defn- wrap-formula-items 
  "If editing an existing formula, this function injects the find-all-formula-items query into a modified 
   request map and returns it, otherwise it simply returns the request as is."
  [req]
  (if (get-in req [:path-params :id])
    (assoc-in req [:params :qfi] (m/find-all-formula-items (Long/parseLong (get-in req [:path-params :id]))))
    req))

(defn edit-formula [req]
  (view/formula-form (-> req wrap-formula-form-data wrap-formula-items)))

(defn save-formula [req]
  (let [data (-> req
                 :params
                 (update :id #(Long/parseLong (empty-to-tempid %))))]
    (m/save-formula data))
  (resp/redirect "/formulas"))

(defn- wrap-nutrients [req]
  (assoc-in req [:params :qn] (m/find-all-nutrients)))

(defn add-formula-item [req]
  (view/add-formula-item-form (-> req wrap-nutrients)))

(defn- wrap-formula-item 
  "Inject formula item form data into the request"
  [req]
   (assoc-in req [:params :q] (m/pull-formula-item (Long/parseLong (get-in req [:path-params :id]))))
  )

(defn edit-formula-item 
  
  [req]
  (view/edit-formula-item-form (-> req wrap-nutrients wrap-formula-item)))

(defn save-formula-item [req]
  (let [data (-> req
                 :params
                 (update :id #(Long/parseLong (empty-to-tempid %)))
                 (update :formula #(Long/parseLong %))
                 (update :nutrient #(Long/parseLong %))
                 (update :milligrams-per-day #(Long/parseLong (empty-to-zero %))))]
    (m/save-formula-item data))
  (resp/redirect (str "/edit-formula/" (get-in req [:params :formula])))
  )

(defn delete-formula-item [req]
  (let [eid (Long/parseLong (get-in req [:path-params :id])) 
        formula-item (m/pull-formula-item eid)
        formula-eid (get-in formula-item [:formula-item/formula :db/id])]
    (m/retract-entity eid)
  (resp/redirect (str "/formulas/" formula-eid))))

(defn batches [req]
  (view/batches (assoc-in req [:params :q] (m/find-all-batches))))

(defn add-batch [req] 
  (view/add-batch-form (assoc-in req [:params :qf] (m/find-all-formulas))))

(defn save-new-batch [req] 
  (let [data (-> req 
                 :params
                 (update :formula #(Long/parseLong %))
                 (update :doses #(Long/parseLong %)))]
    (m/save-new-batch data))
  (resp/redirect "/batches"))

(defn- wrap-batch
  [req] 
    (assoc-in req [:params :qb] (m/find-batch (Long/parseLong (get-in req [:path-params :id]))))
  )

(defn- wrap-batch-items
  [req]
  (assoc-in req [:params :qbi] (m/find-batch-items (Long/parseLong (get-in req [:path-params :id]))))
  )

(defn batch-items 
  [req]
  (view/batch-items (-> req wrap-batch wrap-batch-items))
  )

(defn delete-batch [req]
  (let [eid (Long/parseLong (get-in req [:path-params :id]))]
    (m/retract-entity eid))
  (resp/redirect "/batches"))

(defn- wrap-batch-item
  [req]
  (assoc-in req [:params :qbi] (m/pull-batch-item (Long/parseLong (get-in req [:path-params :id])))))

(defn edit-batch-item [req]
  (view/edit-batch-item-form (-> req wrap-batch-item)))

(defn save-batch-item [req]
  (let [data (-> req
                 :params
                 (update :id #(Long/parseLong %))
                 (update :batch-id #(Long/parseLong %))
                 (update :weight #(Long/parseLong (empty-to-zero %)))
                 (update :complete? #(= "1" %)))]
    (m/save-batch-item data))
  (let [bi (m/pull-batch-item (Long/parseLong (get-in req [:params :id])))]
    (when (:batch-item/complete? bi)
      (-> 
       (hash-map :nid (get-in bi [:batch-item/nutrient :db/id])
                 :new-grams-in-stock (m/subtract-batch-item-from-stock bi))
       (m/update-nutrient-weight))
      ) 
    ) 
  (resp/redirect (str "/batch-items/" (get-in req [:params :batch-id]))))

  
 (comment

   
   
   
   
   )
   
