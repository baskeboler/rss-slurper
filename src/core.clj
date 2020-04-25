(ns core
  (:refer-clojure :exclude [sort find])
  (:require
   [clojure.string  :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.logging :as log]
   [rss-slurper.system :refer [system print-feeds count-items save-feeds]]
   ;; [clojure.pprint :refer [pprint]]
   [com.stuartsierra.component :as component]
   [rss-slurper.nlp :refer [nlp-host]]
   [clojure.repl :refer [set-break-handler!]])
  (:gen-class))

;; (def conn (mg/connect {:port 32768}))
;; (def news-db "news")
;; (declare analyze-text analyze-sentiment)


(def default-urls
  {:observador      {:front-page "https://www.elobservador.com.uy/rss/elobservador.xml"
                     :world      "https://www.elobservador.com.uy/rss/elobservador/mundo.xml"
                     :health     "https://www.elobservador.com.uy/rss/elobservador/salud.xml"}
   :elpais          {:front-page "https://www.elpais.com.uy/rss/"}
   ;; :lanacion        {:front-page "http://contenidos.lanacion.com.ar/herramientas/rss/origen=2"
                     ;; :world      "http://contenidos.lanacion.com.ar/herramientas/rss/categoria_id=7"
                     ;; :politics   "http://contenidos.lanacion.com.ar/herramientas/rss/categoria_id=30"}
   :clarin          {:front-page "https://www.clarin.com/rss/lo-ultimo/"
                     :world      "https://www.clarin.com/rss/mundo/"
                     :politics   "https://www.clarin.com/rss/politica/"}
   :montevideo-com  {:front-page "https://www.montevideo.com.uy/anxml.aspx?58"
                     :tech       "https://www.montevideo.com.uy/anxml.aspx?133"
                     :world      "https://www.montevideo.com.uy/anxml.aspx?59"}
   :ladiaria        {:front-page "https://ladiaria.com.uy/feeds/articulos/"}
   :washington-post {:politics "http://feeds.washingtonpost.com/rss/politics?tid=lk_inline_manual_2&itid=lk_inline_manual_2"
                     :world    "http://feeds.washingtonpost.com/rss/world?tid=lk_inline_manual_13&itid=lk_inline_manual_13"}})

(def ^:export cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 32768
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option (:default is applied first)
   ["-u" "--nlp-url URL" "NLP Server URL"
    :default nlp-host]
   ["-l" "--web-port PORT" "Web server port"
    :default 3000
    :parse-fn #(Integer/parseInt %)]
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc] ; Prior to 0.4.1, you would have to use:
   ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

;; The :default values are applied first to options. Sometimes you might want
;; to apply default values after parsing is complete, or specifically to
;; compute a default value based on other option values in the map. For those
;; situations, you can use :default-fn to specify a function that is called
;; for any options that do not have a value after parsing is complete, and
;; which is passed the complete, parsed option map as it's single argument.
;; :default-fn (constantly 42) is effectively the same as :default 42 unless
;; you have a non-idempotent option (with :update-fn or :assoc-fn) -- in which
;; case any :default value is used as the initial option value rather than nil,
;; and :default-fn will be called to compute the final option value if none was
;; given on the command-line (thus, :default-fn can override :default)

(defn usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: rss-slurper [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  save-feeds    Saves current feeds to db"
        "  print-feeds   Prints current feed"
        "  count-items   Counts the number of news items in the database"
        "  listen        Starts the web server and waits"
        "Please refer to the manual page for more information."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (and
       (= 1 (count arguments))
       (#{"save-feeds" "count-items" "print-feeds" "listen"} (first arguments)))
      {:action (first arguments) :options options}
      :else
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (log/info msg)
  (System/exit status))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok?  0 1) exit-message)

      (let [{:keys [nlp-url port web-port]} options
            sys                             (-> (system {:db-port     port
                                                         :nlp-url     nlp-url
                                                         :rss-url-map default-urls
                                                         :web-port    web-port})
                                       (component/start-system))]
        (set-break-handler! (fn [thread]
                              (component/stop sys)
                              (System/exit 1)))
        (try
          (case action
            "save-feeds"  (save-feeds sys)
            "print-feeds" (print-feeds sys)
            "count-items" (log/info (format "Number of news items in db: %d"
                                            (count-items sys)))
            "listen"      (do
                       (log/info "listening ... ")
                       (.join (-> sys :web :server))))
          (catch Exception e
            (log/error "got an exception: " (ex-message e)))
          (finally
            (component/stop-system sys)))))))


;; (defn- get-news-db [] (mg/get-db conn news-db))


