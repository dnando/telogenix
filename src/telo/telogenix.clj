(ns telo.telogenix
  (:gen-class)
  (:require [org.httpkit.server :as app-server]
            [reitit.ring :as ring]
            ;; [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [telo.controller :as ct]
            [ring.handler.dump :refer [handle-dump]]))

;; Request Routing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn request-info
  "View the information contained in the request, useful for debugging"
  [request]
  (assoc-in request [:params :title] "Home")
  {:status 200
   :body (pr-str request)
   :headers {}})

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get {:handler #'ct/home}}]
     ["/nutrients" {:get {:handler #'ct/nutrients}}]
     ["/edit-nutrient" {:get {:handler #'ct/edit-nutrient}
                        :post {:handler #'ct/save-nutrient}}]
     ["/edit-nutrient/:id" {:get {:parameters {:path {:id int?}}}
                            :handler #'ct/edit-nutrient}]
     ["/del-nutrient/:id" {:get {:parameters {:path {:id int?}}}
                           :handler #'ct/delete-nutrient}]
     ["/formulas" {:get {:handler #'ct/formulas}}]
     ["/edit-formula" {:get {:handler #'ct/edit-formula}
                       :post {:handler #'ct/save-formula}}]
     ["/edit-formula/:id" {:get {:parameters {:path {:id int?}}}
                           :handler #'ct/edit-formula}]
     ["/add-formula-item/:formula-id" {:get {:parameters {:path {:formula-id int?}}}
                                       :handler #'ct/add-formula-item}]
     ["/edit-formula-item/:id" {:get {:parameters {:path {:id int?}}}
                                       :handler #'ct/edit-formula-item}]
     ["/del-formula-item/:id" {:get {:parameters {:path {:id int?}}}
                           :handler #'ct/delete-formula-item}]
     ["/save-formula-item" {:post {:handler #'ct/save-formula-item}}]
     ["/batches" {:get {:handler #'ct/batches}}]
     ["/add-batch" {:get {:handler #'ct/add-batch}}]
     ["/save-new-batch" {:post {:handler #'ct/save-new-batch}}]
     ["/del-batch/:id" {:get {:parameters {:path {:id int?}}}
                           :handler #'ct/delete-batch}]
     ["/batch-items/:id" {:get {:parameters {:path {:id int?}}}
                           :handler #'ct/batch-items}]
     ["/edit-batch-item/:id" {:get {:parameters {:path {:id int?}}}
                                :handler #'ct/edit-batch-item}]
     ["/save-batch-item" {:post {:handler #'ct/save-batch-item}}]
     ["/sign-up" {:post {:handler #'ct/sign-up}}]
     ["/verify-account" {:post {:handler #'ct/verify-account}}]
     ["/login" {:post {:handler #'ct/login}}]
     ["/reset-password" {:post {:handler #'ct/reset-password}}]
     ["/logout" {:post {:handler #'ct/logout}}]
     ["/dump" {:get handle-dump
               :post handle-dump}]]
    {:data {:middleware [parameters/parameters-middleware
                         wrap-keyword-params]}})
   ;; sign-up verify-account login reset-password logout

   (ring/routes
    ;; create-resource-handler serves static files from resources/public
    (ring/create-resource-handler {:path "/"})
    (ring/redirect-trailing-slash-handler {:method :strip})
    (ring/create-default-handler))))

;; System
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Reference to application server instance for stopping/restarting
(defonce app-server-instance (atom nil))


(defn app-server-start
  "Start the application server and log the time of start."

  [http-port]
  (println (str (java.util.Date.)
                " INFO: Starting server on port: " http-port))
  (reset! app-server-instance
          (app-server/run-server #'app {:port http-port})))


(defn app-server-stop
  "Gracefully shutdown the server, waiting 100ms.  Log the time of shutdown"
  []
  (when-not (nil? @app-server-instance)
    (@app-server-instance :timeout 100)
    (reset! app-server-instance nil)
    (println (str (java.util.Date.)
                  " INFO: Application server shutting down..."))))


(defn app-server-restart
  "Convenience function to stop and start the application server"

  [http-port]
  (app-server-stop)
  (app-server-start http-port))


(defn -main
  "Select a value for the http port the app-server will listen to
  and call app-server-start
  The http port is either an argument passed to the function,
  an operating system environment variable or a default value."

  [& [http-port]]
  (let [http-port (Integer. (or http-port (System/getenv "PORT") "8886"))]
    (app-server-start http-port)))


;; REPL driven development helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  
  ;; NOTE: To start the application via the Calva REPL, for inatance, 
  ;; evaluate the files in this order so dependencies are loaded using ctrl-alt-c then enter
  ;; model.clj, view.clj, controller.clj, telogenix.clg
  ;; then you can start or restart the server as needed by evaluating
  ;; the functions below

  ;; Start application server - via `-main` or `app-server-start`
  (-main)
  (app-server-start 8886)

  ;; Stop / restart application server
  (app-server-stop)
  (app-server-restart 8886)

  ;; Get PORT environment variable from Operating System
  (System/getenv "PORT")

  ;; Get all environment variables
  ;; use a data inspector to view environment-variables name
  (def environment-variables
    (System/getenv))

  ;; Check values set in the default system properties
  (def system-properties
    (System/getProperties))
  )

(comment

  (def req {:user {:bar "none"}})
  (assoc-in req [:params :title] "Home")
  (pr-str req)
  (clojure.java.io/resource "public/styles.css")
  
  ,)
