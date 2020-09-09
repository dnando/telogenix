(ns telo.telogenix
  (:gen-class)
  (:require [org.httpkit.server :as app-server]
            [reitit.ring :as ring]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [telo.view :as view]
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
     ["/formulas" {:get ct/formulas}]
     ["/batches" {:get ct/batches}]
     ["/dump" {:get handle-dump
               :post handle-dump}]]
    {:data {:middleware [parameters/parameters-middleware
                         wrap-keyword-params]}})
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
  (clojure.java.io/resource "public/styles.css"))
