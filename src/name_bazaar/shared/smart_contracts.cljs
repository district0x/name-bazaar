(ns name-bazaar.shared.smart-contracts)

(def smart-contracts
  {:auction-offering-factory
   {:name "AuctionOfferingFactory",
    :address "0xce5f6a9591e187d09d0dc0276483db2dab3d0976"},
   :buy-now-offering-library
   {:name "BuyNowOfferingLibrary",
    :address "0x367decc92cbc3054d597c8314475c2f9c9aab47a"},
   :buy-now-offering-factory
   {:name "BuyNowOfferingFactory",
    :address "0x4c3f9d8079c42453efde2cd3b530cbaf3f047010"},
   :registrar
   {:name "Registrar",
    :address "0x542d337975c2f9b436621024df371244bd53a0af"},
   :buy-now-offering
   {:name "BuyNowOffering",
    :address "0x0a41e7d965e87f1c1d341435e9f5871bc475408d"},
   :auction-offering-library
   {:name "AuctionOfferingLibrary",
    :address "0x132e4c91077eca527a4c8136e2c51e13379fd741"},
   :deed
   {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
   :ens
   {:name "ENS", :address "0x7eaf663a2e94548102e46ffa64297f72b164b095"},
   :mock-registrar
   {:name "MockRegistrar",
    :address "0x9efb658ad2c9e11700999c93b859d87e88cba2a1"},
   :offering-registry
   {:name "OfferingRegistry",
    :address "0x8657447dd8b9f047f441efef421c5014b434b860"},
   :district0x-emails
   {:name "District0xEmails",
    :address "0x174d0c7b58ee5a6159b45d3a1c2e185df03dcaa3"},
   :offering-requests
   {:name "OfferingRequests",
    :address "0xdd96309aaf3370222146c72aafbbfdbf02328584"},
   :offering-library
   {:name "OfferingLibrary",
    :address "0x8936ff910fc6244573d5b19683c5423e38cdfd49"},
   :auction-offering
   {:name "AuctionOffering",
    :address "0xf5d61e55e0e6e69845a466ad05585278cfa7f5c7"}})