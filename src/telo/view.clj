(ns telo.view
  (:require [ring.util.response :refer [response]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-js include-css]]
            [hiccup.element :refer [link-to]])
 )

(defn nav [req]
  [:nav {:class "navbar navbar-expand-lg navbar-light bg-light pl-4"}
   [:a {:class "navbar-brand", :href "/"} "Telogenix"]
   [:button {:class "navbar-toggler", :type "button", :data-toggle "collapse", :data-target "#navbarNav", :aria-controls "navbarNav", :aria-expanded "false", :aria-label "Toggle navigation"}
    [:span {:class "navbar-toggler-icon"}]]
   [:div {:class "collapse navbar-collapse", :id "navbarNav"}
    [:ul {:class "navbar-nav"}
     [:li.nav-item 
      [:a.nav-link {:class (when (= (:uri req) "/") "active"), :href "/"} "Home"
       [:span {:class "sr-only"} "(current)"]]]
     [:li.nav-item 
      [:a.nav-link {:class (when (= (:uri req) "/nutrients") "active"), :href "/nutrients"} "Nutrients"]]
     [:li.nav-item 
      [:a.nav-link {:class (when (= (:uri req) "/formulas") "active"), :href "/formulas"} "Formulas"]]
     [:li.nav-item
      [:a.nav-link {:class (when (= (:uri req) "/batches") "active"), :href "/batches"} "Batches"]]
     ]]]
  )

(defn layout
[req content]
  (html5
   {:lang "en"}
   [:head
    [:title (get-in req [:params :title])]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
    ;; <!-- CSS only -->
    [:link {:rel "stylesheet"
            :href "https://stackpath.bootstrapcdn.com/bootstrap/5.0.0-alpha1/css/bootstrap.min.css"
            :integrity "sha384-r4NyP46KrjDleawBgD5tp8Y7UzmLA05oM1iAEQ17CSuDqnUK2+k9luXQOfXJCJ4I"
            :crossorigin "anonymous"}]
    [:link {:rel "stylesheet"
            :href "styles.css"}]]
   [:body
    (nav req)
    [:div.container
     content]
    ;; <!-- JavaScript and dependencies -->
    [:script {:src "https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js"
              :integrity "sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo"
              :crossorigin "anonymous"}]
    [:script {:src "https://stackpath.bootstrapcdn.com/bootstrap/5.0.0-alpha1/js/bootstrap.min.js"
              :integrity "sha384-oesi62hOLfzrys4LxRF63OJCXdXDipiYWBnvTl9Y9/TRlw5xlKIEHpNyvvDShgf/"
              :crossorigin "anonymous"}]
    ])
)


(defn home
  [req]
  (assoc-in req [:params :title] "Home")
  (response
   (layout req '([:h1.display3 "Hello there!"]
                     [:p "what is going on?"]
                     ))))

(defn nutrient-list
  [req]
  (response
   (layout req [:div.wrp
                [:h1.display3
                 [:a.red-plus {:href "/edit-nutrient"} "+"] " Nutrients"]
                [:table.table
                 [:tr
                  [:td "Name"]
                  [:td "Stock (g)"]
                  [:td "Purchase Link"]
                  [:td "Note"]
                  [:td]]
                 (for [n (get-in req [:params :q])]
                   [:tr
                    [:td [:a {:href (str "/edit-nutrient/" (:eid n))}(:name n)]]
                    [:td (:grams-in-stock n)]
                    [:td  [:a {:href (:purchase-url n) :target "_blank"} [:img {:src "bi/link.svg"}]]]
                    [:td (:note n)]
                    [:td [:a.red-x {:href (str "/del-nutrient/" (:eid n))} "x"]]
                    ]
                   )]
                ;;(pr-str (get-in req [:params :q]))
                ])))
;;(pr-str (get-in req [:params :q]))

(defn nutrient-form
  [req]
  (response
   (layout req
           (let [data (first (get-in req [:params :q]))]
           [:div.wrp
            [:h1.display3.mb-4 "Edit Nutrient"]
            [:form {:method "post" :action "/edit-nutrient"}
             [:input {:type "hidden" :name "id" :value (:eid data)}]
             [:div.mb-3
              [:label {:for "theName" :class "form-label"} "Name"]
              [:input {:type "text" :class "form-control" :id "theName" :name "name" :value (:name data)}]]
             [:label {:for "stock" :class "form-label"} "Current Stock"]
             [:div.mb-3.input-group
              [:input {:type "number" :class "form-control" :id "stock" :name "grams-in-stock" :value (:grams-in-stock data)}]
              [:span {:class "input-group-text"} "grams"]]
             [:div.mb-3
              [:label {:for "theNote" :class "form-label"} "Note"]
              [:textarea {:class "form-control" :id "theNote" :name "note"} (:note data)]]
             [:div.mb-3
              [:label {:for "pUrl" :class "form-label"} "Purchase Link"]
              [:input {:type "text" :class "form-control" :id "pUrl" :name "purchase-url" :value (:purchase-url data)}]]
             [:div.mb-3
              [:label {:for "cat" :class "form-label"} "Category"]
              [:select.form-select {:name "category" :id "cat"}
               (for [c (get-in req [:params :qc])]
                 [:option {:value (:catid c) :selected (when (= (:catid c) (:category-eid data)) "selected")} (:name c)])
               ]]
             [:button.btn.btn-primary {:type "submit"} "Save"]]
            (pr-str req)]))))
;; (pr-str (get-in req [:path-params :id]))

(defn formulas
  [req]
  (response
   (layout req [:h1.display3 "Formulas"])))

(defn batches
  [req]
  (response
   (layout req [:h1.display3 "Batches"])))

(defn inspect [req]
  (response
   (layout req [:div (pr-str req)]))
  )
