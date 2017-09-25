pragma solidity ^0.4.14;

/**
 * @title BuyNowOfferingLibrary
 * @dev Contains logic for BuyNowOffering methods
 */

import "OfferingLibrary.sol";

library BuyNowOfferingLibrary {
    using OfferingLibrary for OfferingLibrary.Offering;

    /**
    * @dev Exchanges funds of new owner for ownership of ENS name owner
    * msg.value must exactly equal to offering price
    * @param offering OfferingLibrary.Offering Offering data struct
    */
    function buy(
        OfferingLibrary.Offering storage offering
    ) {
        require(msg.value == offering.price);
        offering.originalOwner.transfer(offering.price);
        offering.transferOwnership(msg.sender);
    }

    /**
    * @dev Changes settings for BuyNowOffering
    * Can be executed only by original owner
    * Can't be executed after ownership was already transferred to a new owner
    * @param offering OfferingLibrary.Offering Offering data struct
    * @param _price uint New price of the offering
    */
    function setSettings(
        OfferingLibrary.Offering storage offering,
        uint _price
    ) {
        require(offering.isSenderOriginalOwner());
        require(!offering.wasOwnershipTransferred());
        offering.price = _price;
        offering.fireOnChanged("setSettings");
    }
}
