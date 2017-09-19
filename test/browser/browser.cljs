(ns browser.browser
  (:require [doo.runner :refer-macros [doo-tests]]
            [browser.key-utils-tests]))

(enable-console-print!)
(doo-tests 'browser.key-utils-tests
           ;; add-more-tests-here
           )
