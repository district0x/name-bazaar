(ns browser.run-tests
  (:require [doo.runner :refer-macros [doo-tests]]
            [browser.encryption-tests]))

(enable-console-print!)

(doo-tests 'browser.encryption-tests)
