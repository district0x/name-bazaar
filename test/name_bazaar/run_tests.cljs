(ns name-bazaar.run-tests
  (:require
    [cljs.nodejs :as nodejs]
    [name-bazaar.smart-contract-tests]
    [name-bazaar.smart-contract-ext-tests]
    [cljs.test :refer-macros [run-tests]]))

(nodejs/enable-util-print!)
(set! (.-error js/console) (fn [x] (.log js/console x)))

(comment
  (run-tests 'name-bazaar.smart-contract-tests
             'name-bazaar.smart-contract-ext-tests))

(defn -main [& _]
  (run-tests 'name-bazaar.smart-contract-tests
             'name-bazaar.smart-contract-ext-tests))

(set! *main-cli-fn* -main)

