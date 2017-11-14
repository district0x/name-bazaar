(ns name-bazaar.ui.pages.about-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [soda-ash.core :as ui]))

(defn- a [href text]
  [:a {:target :_blank
       :href href}
   text])

(defmethod page :route/about []
  [app-layout {:meta {:title "NameBazaar - About"}}
   [ui/Segment
    [:h1.ui.header.padded "About"]
    [:div.padded
     [:p "The Name Bazaar launched in October of 2017 by the district0x team as the second district on the " [a "https://district0x.io/" "district0x network"] ". The Name Bazaar is an open source project, governed by the users of the platform itself. It is built using Clojurescript served by IPFS for the front end and Solidity running on the Ethereum blockchain for the backend."]
     [:p "We welcome all contributions from the public. If you have any thoughts or suggestions on how we can improve, please stop by " [a "https://github.com/district0x/name-bazaar" "Name Bazaar’s github"] " or join the " [a "https://www.reddit.com/r/district0x/" "district0x Reddit"] ", " [a "https://t.me/district0x" "Telegram"] ", or " [a "https://district0x.chat/" "Rocket Chat"] " to chat with the creators and community."]
     [:h1.ui.header "What is district0x?"]
     [:p "district0x is a network of marketplaces and communities that exist as decentralized autonomous organizations on the district0x Network. All internet citizens will be able to deploy districts to the network free of charge, forever. The first district deployed; " [a "https://ethlance.com/" "Ethlance"] ", is a decentralized job market which allows companies to hire and workers to find opportunities. Name Bazaar is the the second district launched; with more to follow."]
     [:h1.ui.header "More Information About district0x"]
     [:p "The districts launched by district0x are built on a standard open source framework comprised of Ethereum smart contracts and front-end libraries, called d0xINFRA. This powers the various districts with core functionalities that are necessary to operate an online marketplace or community; including the ability for users to post listings, filter and search those listings, rank peers and gather reputation, send invoices and collect payments. The framework is designed to be open and extendable, allowing districts to be customized and granted additional functionality through the use of auxiliary modules - which can be thought of like “plug-ins” or “extensions.” District0x is powered by Ethereum, Aragon, and IPFS."]
     [:p "district0x has it’s own token, " [a "https://coinmarketcap.com/currencies/district0x/" "DNT"] ", which is used as a means of facilitating open participation and coordination on the network. DNT can be used to signal what districts should be built and deployed by the district0x team and can be staked to gain access to voting rights in any district on the district0x Network."]
     [:p "Perhaps the coolest thing about district0x is that you don't need to know how to code or have any technical skill to launch a district. If you dream it - the team can build it! district0x makes use of a " [a "https://github.com/district0x/district-proposals" "district proposal process"] " to allow the community to determine what districts they would like to see built and deployed to the network next by the district0x team. Winning proposals are " [a "https://vote.district0x.io/" "voted on"] " by the community using DNT."]]]])
