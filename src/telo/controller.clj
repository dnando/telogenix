(ns telo.controller
  (:require
   [ring.util.response :as resp]
   [telo.view :as view]
   ))


(defn home [req]
  (view/home req))

(defn nutrients [req]
  (view/nutrient-list req))

(defn edit-nutrient [req]
  (view/nutrient-form req))

(defn save-nutrient [req]
  ;; validation and db interation 
  ;; if valid, redirect to nutrient list
  ;; else redirect to edit-nutrient
  (resp/redirect "/nutrients"))

(defn formulas [req]
  (view/formulas req))

(defn batches [req]
  (view/batches req))
  
 