pragma solidity ^0.4.14;

/**
 * @title BuyNowOffering
 * @dev Extends Offering with BuyNow functionality
 * Logic for contract methods is at BuyNowOfferingLibrary, so logic is not duplicated each time user creates
 * new offering. It saves large amounts of gas. BuyNowOfferingLibrary address is linked to BuyNowOffering contract
 * at compilation time.
 */

import "Offering.sol";
import "BuyNowOfferingLibrary.sol";

contract BuyNowOffering is Offering {

    function BuyNowOffering(
        address _offeringRegistry,
        address _registrar,
        bytes32 _node,
        string _name,
        bytes32 _labelHash,
        address _originalOwner,
        address _emergencyMultisig,
        uint _price
    )
        Offering(_offeringRegistry, _registrar, _node, _name, _labelHash, _originalOwner, _emergencyMultisig, 1, _price)
    {
    }

    function buy() payable {
        BuyNowOfferingLibrary.buy(offering);
    }

    function setSettings(uint _price) {
        BuyNowOfferingLibrary.setSettings(offering, _price);
    }

    function() payable {
        buy();
    }
}
