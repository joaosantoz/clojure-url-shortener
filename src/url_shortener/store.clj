(ns url-shortener.store
  "Armazenamento em memoria dos links encurtados.

  O estado fica em um atom com o formato:
    {:counter 3
     :links   {\"0\" \"https://a.com\" \"1\" \"https://b.com\" \"2\" \"https://c.com\"}}")

(def ^:private alphabet
  "Alfabeto base62 usado para gerar os codigos curtos."
  "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

(def ^:private url-pattern
  #"(?i)https?://\S+")

(defn encode-base62
  "Converte um inteiro nao-negativo em uma string base62.

  (encode-base62 0)  => \"0\"
  (encode-base62 61) => \"Z\"
  (encode-base62 62) => \"10\""
  [n]
  (cond
    (neg? n)  (throw (IllegalArgumentException. "n deve ser >= 0"))
    (zero? n) (str (first alphabet))
    :else     (loop [n n, acc ""]
                (if (zero? n)
                  acc
                  (recur (quot n 62)
                         (str (nth alphabet (rem n 62)) acc))))))

(defn valid-url?
  "Diz se a string parece uma URL http ou https."
  [s]
  (boolean (and (string? s)
                (seq s)
                (re-matches url-pattern s))))

(defn new-store
  "Cria um store vazio."
  []
  (atom {:counter 0 :links {}}))

(defn- find-existing-id
  "Procura um codigo ja emitido para a mesma URL."
  [links url]
  (some (fn [[id registrada]]
          (when (= registrada url) id))
        links))

(defn shorten!
  "Encurta uma URL. A mesma URL sempre devolve o mesmo codigo.

  Devolve {:id ... :url ... :created? bool} ou {:error :invalid-url}."
  [store url]
  (if-not (valid-url? url)
    {:error :invalid-url}
    (if-let [existing (find-existing-id (:links @store) url)]
      {:id existing :url url :created? false}
      (let [{:keys [last-id]}
            (swap! store
                   (fn [{:keys [counter links]}]
                     (let [id (encode-base62 counter)]
                       {:counter (inc counter)
                        :links   (assoc links id url)
                        :last-id id})))]
        {:id last-id :url url :created? true}))))

(defn resolve-id
  "Devolve a URL original de um codigo, ou nil se ele nao existir."
  [store id]
  (get-in @store [:links id]))

(defn list-links
  "Devolve o mapa completo de codigo -> URL."
  [store]
  (:links @store))

(defn count-links
  "Quantidade de links cadastrados."
  [store]
  (count (:links @store)))
