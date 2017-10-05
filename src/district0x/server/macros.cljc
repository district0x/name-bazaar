(ns district0x.server.macros
  #?(:cljs (:require [district0x.server.logging :as logging]))
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]])))

(defmacro gotry [& body] 
   `(cljs.core.async.macros/go 
      (try 
        ~@body 
        (catch js/Object e# 
          (logging/error "An exception has occured" {:error e#})))))
