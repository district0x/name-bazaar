(ns name-bazaar.cmd
  (:require
    [cljs.nodejs :as nodejs]
    [name-bazaar.tests]
    [cljs.test :refer-macros [run-tests]]))

(nodejs/enable-util-print!)
(set! js/window #js {})

(set! (.-error js/console) (fn [x] (.log js/console x)))

(comment
  (run-tests 'name-bazaar.tests))

(defn -main [& _]
  (run-tests 'name-bazaar.tests))

(set! *main-cli-fn* -main)

