(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85"}
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0x4976fb03C32e5B8cfe2b6cCB31c09Ba78EBaBa41"}
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0x084b1c3C81545d370f3634392De611CaaBFf8148"}
       :offering-registry {:name "OfferingRegistry" :address "0xDe143e7eE5CD9488EFF68D386304335bc2806269"}
       :buy-now-offering {:name "BuyNowOffering" :address "0x369952536Bd65A096755D1494038d6074A816943"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0xE49b5Ca05310d21EDB1A57C5b9B6695E77903508"}
       :auction-offering {:name "AuctionOffering" :address "0x220aFd01F53887d1DD03052a12c720F58C111675"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0x84e6E9d2bAC10C3d335Abf81E69ac9Ea42efec7C"}
       :district0x-emails {:name "District0xEmails" :address "0x7364f83085A2c20263830cB6EEc5901C6EdDaA46"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x0FDB292129057eeA228B235f5E5c19d59a007404"}})