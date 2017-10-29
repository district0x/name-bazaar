(ns name-bazaar.ui.pages.how-it-works-page
  (:require
    [district0x.ui.components.misc :refer [page youtube]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.utils :refer [path-for]]
    [soda-ash.core :as ui]))

(defn- a [href text]
  [:a {:target :_blank
       :href href}
   text])

(defmethod page :route/how-it-works []
  [app-layout
   [ui/Segment
    {:class "how-it-works-page"}
    [:h1.ui.header.padded "How it works"]
    [:div.padded
     [:p "At its core, Name Bazaar allows peer-to-peer, trust-less exchange of cryptographic assets on the blockchain in the form of “names”, or " [a "https://ens.domains" "Ethereum Name Service"] " (ENS) domains."]
     [:p "Name Bazaar provides the ability for any user with a name for sale to generate unique smart contracts that allow transfer of ownership of that name to a new Ethereum address and owner, and receive payment in return. Any user with Ethereum looking to buy a name can do so through these contracts by interacting with the website."]
     [:p "No registration or sign up is required. All you need is an Ethereum address available through the " [a "https://metamask.io/" "MetaMask"] " or " [a "https://github.com/paritytech/parity-extension/releases" "Parity"] " browser extensions. Every time you take an action that requires confirmation on the blockchain, you will be asked to confirm that transaction via MetaMask or Parity. This will require a small fee in Ethereum to perform the operation (this fee goes to the Ethereum Network, not to Name Bazaar)."]
     [:h1.ui.header "Buying Names"]
     [:p.centered
      [youtube
       {:src "https://www.youtube.com/embed/pmtVMaqk_eg"}]]
     [:p "There’s a persistent search bar at the top of the website, no matter what page you’re on. Simply type in a name you’re looking for and you’re off to the races! By clicking the “Offerings” button at the top of the left-hand menu, a more " [:a {:href (path-for :route.offerings/search)} "detailed offering search page"] " is available, where you can filter by a number of intuitive parameters like price, buy now vs. auction, and so on. "]
     [:p.centered [:div.flow-image.buy]]
     [:p "Once you’ve found a name you’d like to buy or bid on, click on it for more information. If the offering is a “buy now” type, you can immediately pay the listed price to acquire the name. If it’s an “auction” type, you can enter a winning bid, and watch the name to be notified if you get out bid, or if the auction closes."]
     [:h1.ui.header "Selling Names"]
     [:p.centered
      [youtube
       {:src "https://www.youtube.com/embed/xyB4q1i51xI"}]]
     [:p "Any names already registered and owned by a MetaMask or Parity address can be listed for sale on the site through a simple " [:a {:href (path-for :route.offerings/create)} "Create Offering"] " page. When creating an auction, type the name you’d like to sell, the starting price, the “Minimum Bid Increase”, and the time you’d like the auction to end. You can also adjust the “Time Extension” slider, to allow the auction to continue in case any new bids roll in at the last minute."]
     [:p.centered [:div.flow-image.sell]]
     [:p "In addition to the auction feature, sellers can also utilize the “Buy Now” listing type, which is a firm set price to immediately buy the name."]
     [:h1.ui.header "Watching and Requesting Names"]
     [:p.centered
      [youtube
       {:src "https://www.youtube.com/embed/CMKyiU2qco8"}]]
     [:p "Still have questions? Join the district0x " [a "https://www.reddit.com/r/district0x" "Reddit"] ", " [a "https://t.me/district0x" "Telegram"] ", or " [a "https://district0x.chat/" "Rocket Chat"] " to chat with the creators and community."]
     [:p "Found a bug or have an improvement in mind? Open up an issue in Name Bazaar's " [a "https://github.com/district0x/name-bazaar" "Github"] " and we'll be sure to take a look!"]]]])
