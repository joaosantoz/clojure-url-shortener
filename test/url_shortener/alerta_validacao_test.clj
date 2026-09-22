(ns url-shortener.alerta-validacao-test
  "Teste temporario, usado apenas para validar o canal de alertas do pipeline.

  Ele falha de proposito para que o workflow CI termine em failure e o workflow
  Alertas abra a issue de alerta notificando o responsavel. Depois de validado,
  este arquivo deve ser removido."
  (:require [clojure.test :refer [deftest is testing]]))

(deftest falha-proposital-para-validar-o-alerta
  (testing "quebra intencional do pipeline"
    (is (= :canal-de-alertas :funcionando)
        "Falha proposital: serve so para disparar o alerta de pipeline quebrado.")))
