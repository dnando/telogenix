(ns telo.controller
  (:require
   [ring.util.response :as resp]
   [telo.view :as view]
   [telo.model :as m]
   ))

;; helper functions
(defn empty-to-zero [v]
  (if (empty? v)
    "0"
    v))

(defn empty-to-tempid [v]
  (if (empty? v)
    "-1"
    v))

(defn nutrient-form-data [req]
  (if (get-in req [:path-params :id])
    (assoc-in req [:params :q] (m/pull-nutrient (Long/parseLong (get-in req [:path-params :id]))))
    (assoc-in req [:params :q] {:db/id -1 :nutrient/name "" :nutrient/grams-in-stock "0" :nutrient/purchase-url "" :nutrient/note ""})))

(defn category-data [req]
  (assoc-in req [:params :qc] (m/find-all-categories)))

;; controller functions
(defn home [req]
  (view/home req))

(defn nutrients [req]
  (view/nutrient-list (assoc-in req [:params :q] (m/find-all-nutrients))))

(defn edit-nutrient [req]
  (view/nutrient-form (-> req nutrient-form-data category-data)))

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
    (m/retract-nutrient eid))
  (resp/redirect "/nutrients"))

(defn formulas [req]
  (view/formulas req))

(defn batches [req]
  (view/batches req))


  
 (comment

   
   
   
   
   )
   
