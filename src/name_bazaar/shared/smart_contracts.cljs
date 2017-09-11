(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x363607e1c753771922f1eae04cab76bd83a56f84"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x37250948e4d4da0e48d62c39d1d40b8b3eb10235"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xfab3520ec7880e7f5bde9f5843ee6059d85714a4"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x01c0419cf58a5d6c5f76a187f9c9624a222292ed"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xe52192fa2b98bc9411402fdba02bab93a4da1f19"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0xe7b1147927e7b2474295b3fbbb063e88cab17b36"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x81f59848facb5ae67efbee7aba1ba5fb794bdfa8"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x233fafcd28f17051cf9033483675366377aad675"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x6a5c471dfd12a11f8af23f1b6e3f4bf6a2ca8df1"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})