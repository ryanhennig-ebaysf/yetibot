(ns yetibot.adapters.irc
  (:require
    [yetibot.chat]
    [irclj
     [core :as irc]
     [connection :as irc-conn]]
    [yetibot.models.users :as users]
    [clojure.string :refer [split-lines]]
    [yetibot.util :refer [env conf-valid? make-config]]
    [yetibot.chat :refer [send-msg-for-each]]
    [yetibot.util.format :as fmt]
    [yetibot.handler :refer [handle-raw]]))

(def config (make-config [:IRC_HOST :IRC_USERNAME :IRC_CHANNELS]))

(declare conn)

(def chat-source (format "irc/%s" (:IRC_CHANNELS env)))

(defn send-msg [msg]
  (irc/message conn (:IRC_CHANNELS env) msg))

(defn- create-user [info]
  (let [username (:nick info)
        id (:user info)]
    (users/create-user username (merge info {:id id}))))

(defn send-paste
  "In IRC there are new newlines. Each line must be sent as a separate message, so
   split it and send one for each"
  [p] (send-msg-for-each (split-lines p)))

(defn fetch-users []
  (irc-conn/write-irc-line conn "WHO" (:IRC_CHANNELS env)))

(def messaging-fns
  {:msg send-msg
   :paste send-paste})

(defn handle-message [_ info]
  (let [user-id (:user info)
        user (users/get-user chat-source user-id)]
    (binding [yetibot.chat/*messaging-fns* messaging-fns]
      (handle-raw chat-source user :message (:text info)))))

(defn handle-part [_ info]
  (handle-raw chat-source
              (create-user info) :leave nil))

(defn handle-join [_ info]
  (handle-raw chat-source
              (create-user info) :enter nil))

(defn handle-nick [_ info]
  (create-user info)
  (prn "NICK" info))

(defn handle-who-reply [_ info]
  (prn "352" info)
  (let [{[_ _ user _ _ nick] :params} info]
    (prn "add user" user nick)
    (users/add-user chat-source
                    (create-user {:user user :nick nick}))))

(defn raw-log [a b c] (prn b c))

(defn handle-end-of-names
  "Callback for end of names list from IRC. Currently not doing anything with it."
  [irc event]
  (let [users (-> @irc :channels vals first :users)]))

(def callbacks {:privmsg #'handle-message
                :raw-log #'raw-log
                :part #'handle-part
                :join #'handle-join
                :nick #'handle-nick
                :366 #'handle-end-of-names
                :352 #'handle-who-reply})

; only try connecting when config is present
(defonce conn
  (when (conf-valid? config)
    (irc/connect (:IRC_HOST config) (read-string (or (:IRC_PORT env) "6667")) (:IRC_USERNAME config)
                 :callbacks callbacks)))

(defn start
  "Join and fetch all users with WHO <channel>"
  []
  (when conn
    (irc/join conn (:IRC_CHANNELS config))
    (fetch-users)))

(defn part []
  (when conn
    (irc/part conn (:IRC_CHANNELS config))))
