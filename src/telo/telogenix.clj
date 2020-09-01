(ns telo.telogenix
  (:gen-class)
  (:require [org.httpkit.server :as app-server]
            ;;[ring.middleware.defaults :as wrap-defaults]
            ;;[ring.middleware.params :refer [wrap-params]]
            ;;[ring.middleware.reload :refer [wrap-reload]]
            [reitit.ring :as ring]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
           ;; [compojure.core :refer [defroutes GET POST]]
            [telo.view :as view]
            [telo.controller :as ct]
            [ring.handler.dump :refer [handle-dump]]
            
            ))

;; Request Routing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn request-info
  "View the information contained in the request, useful for debugging"
  [request]
  (assoc-in request [:params :title] "Home")
  {:status 200
   :body (pr-str request)
   :headers {}})

;; Compojure
;;(defroutes routes
;;  (GET "/" [] handler/home)
;;  (GET "/nutrients" [] handler/nutrient-list)
;;  (GET "/edit-nutrient" [] handler/edit-nutrient)
;;  (GET "/formulas" [] handler/formulas)
;;  (GET "/batches" [] handler/batches)  
;;  (GET "/info" [] request-info)
;;)

;; This is how we apply middleware to the application in compojure
;;(def app
;;  (wrap-params
;; routes))
;; 
;; Here is a sample of a redirect
;; (ring.util.response/redirect "https://ring-clojure.github.io/ring/")

;; reitet

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get ct/home}]
     ["/nutrients" {:get ct/nutrients}]
     ["/edit-nutrient" {:get ct/edit-nutrient
                        :post ct/save-nutrient}]
     ["/formulas" {:get ct/formulas}]
     ["/batches" {:get ct/batches}]
     ["/dump" {:get handle-dump}]
     ]
    {:data {:middleware [parameters/parameters-middleware 
                         ]}}
    )
   
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler)
    )
   ))
 ;; (ring/redirect-trailing-slash-handler {:method :strip})
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
    (System/getProperties)))

(comment

  (def req {:user {:bar "none"}})
  (assoc-in req [:params :title] "Home")
  (pr-str req)
  (clojure.java.io/resource "public/styles.css")
  )
