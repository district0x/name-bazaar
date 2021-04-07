(ns name-bazaar.ui.components.offering.general-info
  (:require
    [clojure.string :as string]
    [district0x.shared.utils :refer [epoch->long empty-address?]]
    [district0x.ui.components.misc :refer [etherscan-link]]
    [district0x.ui.utils :refer [format-time-duration-units format-eth-with-code format-time-duration-units format-local-datetime time-ago]]
    [name-bazaar.ui.utils :refer [path-for strip-root-registrar-suffix]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn offering-original-owner-line [{:keys [:offering/original-owner :offering/address]}]
  (let [resolved-address @(subscribe [:reverse-resolved-address original-owner])]
    [:div.ellipsis
     "Offered by"
     (when @(subscribe [:offering/active-address-original-owner? address]) " (You)")
     ": "
     [:a
      {:href (path-for :route.user/offerings {:user/address (strip-root-registrar-suffix resolved-address)})}
      resolved-address]]))

(defn offering-new-owner-line [{:keys [:offering/new-owner :offering/address]}]
  (let [active-address-new-owner? @(subscribe [:offering/active-address-new-owner? address])
        resolved-address @(subscribe [:reverse-resolved-address new-owner])]
    [:div.ellipsis
     [:span
      {:class (when active-address-new-owner? :purple)}
      "Purchased by"
      (when active-address-new-owner? " you")
      ": "]
     [:a
      {:href (path-for :route.user/purchases {:user/address (strip-root-registrar-suffix resolved-address)})}
      resolved-address]]))

(defn offering-address-line [{:keys [:offering/address]}]
  [:div.ellipsis
   "Offering Address: " [etherscan-link {:address address} address]])


(defn registrar-registration-owner [{:keys [:eth-registrar-registration]}]
  (let [{:keys [:eth-registrar.registration/owner]} eth-registrar-registration]
    [:div
     "Registration Owner: " [etherscan-link
                             {:address owner}]]))

(defn auction-offering-winning-bidder-line [{:keys [:auction-offering/winning-bidder]}]
  (let [resolved-address @(subscribe [:reverse-resolved-address winning-bidder])]
    [:div.ellipsis "Winning bidder: "
     (if winning-bidder
       [:a
        {:href (path-for :route.user/bids {:user/address (strip-root-registrar-suffix resolved-address)})}
        resolved-address]
       "none")]))

(defn offering-created-on-line [{:keys [:offering/created-on]}]
  [:div.ellipsis
   "Created on: " (format-local-datetime created-on)
   "(" (time-ago created-on) ")"])

(defn offering-finalized-on-line [{:keys [:offering/finalized-on]}]
  [:div.ellipsis
   "Finalized on: " (format-local-datetime finalized-on)
   "(" (time-ago finalized-on) ")"])

(defn offering-name-line [{:keys [:offering/name]}]
  [:div "Name: " (when name
                   [:a
                    {:href (path-for :route.ens-record/detail {:ens.record/name name})}
                    name])])

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
                :offering/new-owner :offering/finalized-on :offering/auction? :auction-offering/end-time
                :auction-offering/min-bid-increase :auction-offering/extension-duration
                :auction-offering/winning-bidder :offering/top-level-name?]} offering
        registrar-registration @(subscribe [:offering/registrar-registration address])]
    [:div.description.ellipsis
     (dissoc props :offering)
     [offering-name-line
      {:offering/name name}]
     [offering-created-on-line
      {:offering/created-on created-on}]
     (when auction?
       [offering-auction-end-time-line
        {:offering offering}])
     (when auction?
       [:div "Min. Bid Increase: " (format-eth-with-code min-bid-increase)])
     (when auction?
       [:div "Time Extension: "
        (format-time-duration-units (epoch->long extension-duration))])
     (when (and auction? (not finalized-on))
       [auction-offering-winning-bidder-line
        {:auction-offering/winning-bidder winning-bidder}])
     (when finalized-on
       [offering-finalized-on-line
        {:offering/finalized-on finalized-on}])
     [offering-original-owner-line
      {:offering/original-owner original-owner
       :offering/address address}]
     (when-not (empty-address? new-owner)
       [offering-new-owner-line
        {:offering/new-owner new-owner
         :offering/address address}])
     [offering-address-line
      {:offering/address address}]
     (when top-level-name?
       [registrar-registration-owner
        {:eth-registrar-registration registrar-registration}])]))
