(ns yetibot.webapp.views.common
  (:require
    [yetibot.core.version :refer [version]]
    [hiccup.page :refer [include-css include-js html5]]
    [hiccup.element :refer :all]))

(def title "YetiBot – A command line in your Campfire")

(defn layout [& content]
  (html5
    [:head
      [:title title]
      (include-js "http://code.jquery.com/jquery-1.8.1.min.js" "/js/main.js")
      (include-css "/css/screen.css")]
    [:body
      [:div.content.animate
        (link-to "http://github.com/devth/yetibot"
                 (image {:class "yeti animate"} "/img/yeti.png")
                 [:h1 "yetibot"]
                 [:p version] )]]))
