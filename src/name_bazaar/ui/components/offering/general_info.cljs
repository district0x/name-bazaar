(ns name-bazaar.ui.components.offering.general-info
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils :refer [epoch->long empty-address?]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.text-field :refer [ether-field-with-currency]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code format-time-duration-units format-local-datetime time-ago]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn offering-original-owner-line [{:keys [:offering/original-owner :offering/address]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Offered by"
   (when @(subscribe [:offering/active-address-original-owner? address]) " (You)")
   ": "
   [a {:route :route.user/offerings
       :route-params {:user/address original-owner}}
    original-owner]])

(defn offering-address-line [{:keys [:offering/address]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Offering Address: " [d0x-misc/etherscan-link {:address address} address]])


(defn registrar-entry-deed-value-line [{:keys [:registrar-entry]}]
  (let [{:keys [:registrar.entry.deed/address :registrar.entry.deed/value]} registrar-entry]
    [:div
     "Locked Value: " [d0x-misc/etherscan-link
                       {:address address}
                       (format-eth-with-code value)]]))

(defn auction-offering-winning-bidder-line [{:keys [:auction-offering/winning-bidder]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Winning bidder: " (if winning-bidder
                        [a {:route :route.user/bids
                            :route-params {:user/address winning-bidder}}
                         winning-bidder]
                        "none")])

(defn offering-created-on-line [{:keys [:offering/created-on]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Created on: " (format-local-datetime created-on)
   "(" (time-ago created-on) ")"])

(defn offering-name-line [{:keys [:offering/name]}]
  [:div "Name: " [a {:route :route.ens-record/detail
                     :route-params {:ens.record/name name}}
                  name]])

(defn offering-auction-end-time-line [{:keys [:offering]}]
  (let [{:keys [:offering/address :auction-offering/end-time]} offering
        ended? @(subscribe [:auction-offering/end-time-ended? address])
        time-remaining-biggest-unit @(subscribe [:auction-offering/end-time-countdown-biggest-unit address])]
    [:div
     (if ended?
       "Ended on: "
       "Ends on: ")
     (format-local-datetime end-time)
     (when-not ended?
       [:span " (" time-remaining-biggest-unit " left)"])]))

(defn offering-general-info [{:keys [:offering] :as props}]
  (let [{:keys [:offering/name :offering/created-on :offering/address :offering/original-owner
                :offering/type :auction-offering/end-time :auction-offering/min-bid-increase
                :auction-offering/extension-duration :auction-offering/winning-bidder]} offering
        registrar-entry @(subscribe [:offering/registrar-entry address])]
    [:div
     (dissoc props :offering)
     [offering-name-line
      {:offering/name name}]
     [offering-created-on-line
      {:offering/created-on created-on}]
     (when (= type :auction-offering)
       [offering-auction-end-time-line
        {:offering offering}])
     (when (= type :auction-offering)
       [:div "Min. Bid Increase: " (format-eth-with-code min-bid-increase)])
     (when (= type :auction-offering)
       [:div "Time Extension: "
        (d0x-ui-utils/format-time-duration-units (epoch->long extension-duration))])
     (when (= type :auction-offering)
       [auction-offering-winning-bidder-line
        {:auction-offering/winning-bidder winning-bidder}])
     [offering-original-owner-line
      {:offering/original-owner original-owner
       :offering/address address}]
     [offering-address-line
      {:offering/address address}]
     [registrar-entry-deed-value-line
      {:registrar-entry registrar-entry}]]))
