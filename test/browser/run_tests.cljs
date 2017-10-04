(ns browser.run-tests
  (:require [browser.encryption-tests]
            [doo.runner :refer-macros [doo-tests]]))

(enable-console-print!)

(doo-tests 'browser.encryption-tests
           ;; add-more-tests-here
           )
