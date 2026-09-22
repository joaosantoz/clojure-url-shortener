(ns url-shortener.handler
  "Roteamento HTTP da aplicacao, no formato Ring."
  (:require [cheshire.core :as json]
            [url-shortener.store :as store]))

(def app-version "1.1.0")

(def ^:private short-code-pattern #"/([A-Za-z0-9]+)")

(defn- json-response
  [status body]
  {:status  status
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body    (json/generate-string body)})

(defn- read-json-body
  "Le o corpo da requisicao como JSON. Devolve nil se o corpo for invalido."
  [request]
  (try
    (some-> (:body request) slurp (json/parse-string true))
    (catch Exception _ nil)))

(defn- resolve-base-url
  "URL publica do servico. A opcao :base-url tem prioridade sobre o host da
  propria requisicao."
  [request configurada]
  (if (and (string? configurada) (seq configurada))
    configurada
    (str (name (or (:scheme request) :http))
         "://"
         (get-in request [:headers "host"] "localhost:3000"))))

(defn- handle-shorten
  [store request configurada]
  (let [corpo (read-json-body request)]
    (if-not (map? corpo)
      (json-response 400 {:error "Corpo invalido. Envie um JSON como {\"url\": \"https://...\"}"})
      (let [url    (:url corpo)
            result (store/shorten! store url)]
        (if (:error result)
          (json-response 400 {:error    "URL invalida. Informe algo comecando com http:// ou https://"
                              :recebido url})
          (json-response (if (:created? result) 201 200)
                         {:id        (:id result)
                          :url       (:url result)
                          :short_url (str (resolve-base-url request configurada) "/" (:id result))
                          :novo      (:created? result)}))))))

(defn- handle-redirect
  [store id]
  (if-let [destino (store/resolve-id store id)]
    {:status  302
     :headers {"Location" destino}
     :body    ""}
    (json-response 404 {:error "Codigo nao encontrado" :id id})))

(defn app
  "Constroi o handler Ring a partir de um store.

  Opcoes aceitas:
    :base-url  prefixo fixo usado ao montar o campo short_url."
  ([store] (app store {}))
  ([store opts]
   (let [configurada (:base-url opts)]
     (fn [request]
       (let [uri    (:uri request)
             method (:request-method request)]
         (cond
           (and (= :get method) (= "/" uri))
           (json-response 200 {:service   "url-shortener"
                               :version   app-version
                               :endpoints ["GET  /health"
                                           "POST /shorten"
                                           "GET  /links"
                                           "GET  /{codigo}"]})

           (and (= :get method) (= "/health" uri))
           (json-response 200 {:status "ok" :links (store/count-links store)})

           (and (= :get method) (= "/links" uri))
           (json-response 200 {:total (store/count-links store)
                               :links (store/list-links store)})

           (and (= :post method) (= "/shorten" uri))
           (handle-shorten store request configurada)

           (= :get method)
           (if-let [[_ id] (re-matches short-code-pattern uri)]
             (handle-redirect store id)
             (json-response 404 {:error "Rota nao encontrada" :uri uri}))

           :else
           (json-response 405 {:error "Metodo nao permitido" :metodo (name method)})))))))
