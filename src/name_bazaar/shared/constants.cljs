(ns name-bazaar.shared.constants)

(def routes
  ["/" [[["name/" :ens.record/name] :route.ens-record/detail]
        ["register" :route.registrar/register]
        ["watched-names" :route/watched-names]
        [["user/" :user/address "/offerings"] :route.user/offerings]
        [["user/" :user/address "/purchases"] :route.user/purchases]
        [["user/" :user/address "/bids"] :route.user/bids]
        ["my-settings" :route.user/my-settings]
        ["my-offerings" :route.user/my-offerings]
        ["my-purchases" :route.user/my-purchases]
        ["my-bids" :route.user/my-bids]
        ["offerings/create" :route.offerings/create]
        [["offerings/" :offering/address] :route.offerings/detail]
        [["offerings/" :offering/address "/edit"] :route.offerings/edit]
        ["offerings" :route.offerings/search]
        ["offering-requests" :route.offering-requests/search]
        ["about" :route/about]
        ["how-it-works" :route/how-it-works]
        [true :route/home]]])

