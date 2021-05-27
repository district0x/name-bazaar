(ns name-bazaar.shared.constants)

(def min-name-length 3)

(defn supported-tld-length? [name]
  (>= (count name) min-name-length))

(def empty-label-hash "0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470")

(def routes
  ["/" [[["name/" :ens.record/name] :route.ens-record/detail]
        ;; TODO registrations support over new eth registrar not yet available
        ;["register" :route.registrar/register]
        ["instant-registration" :route.registrar/instant-registration]
        ["watched-names" :route/watched-names]
        [["user/" :user/address "/offerings"] :route.user/offerings]
        [["user/" :user/address "/purchases"] :route.user/purchases]
        [["user/" :user/address "/bids"] :route.user/bids]
        ["my-settings" :route.user/my-settings]
        ["my-offerings" :route.user/my-offerings]
        ["my-purchases" :route.user/my-purchases]
        ["my-bids" :route.user/my-bids]
        ["manage-names" :route.user/manage-names]
        ["offerings/create" :route.offerings/create]
        [["offerings/" :offering/address] :route.offerings/detail]
        [["offerings/" :offering/address "/edit"] :route.offerings/edit]
        ["offerings" :route.offerings/search]
        ["about" :route/about]
        ["how-it-works" :route/how-it-works]
        [true :route/home]]])
