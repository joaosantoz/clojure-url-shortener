(ns build
  "Script de build do projeto. Use `clojure -T:build uber` para gerar o uberjar."
  (:require [clojure.tools.build.api :as b]))

(def lib 'br.pucpr/url-shortener)
(def version "1.0.0")
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn- project-basis []
  (b/create-basis {:project "deps.edn"}))

(defn clean
  "Remove os artefatos gerados em target/."
  [_]
  (b/delete {:path "target"})
  (println "Limpou target/"))

(defn uber
  "Compila o projeto e empacota tudo em um jar executavel."
  [_]
  (clean nil)
  (let [basis (project-basis)]
    (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
    (b/compile-clj {:basis basis :src-dirs ["src"] :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis     basis
             :main      'url-shortener.core}))
  (println "Gerou" uber-file))
