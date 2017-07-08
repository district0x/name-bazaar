pragma solidity ^0.4.11;

import "Offering.sol";
import "./InstantBuyOfferingLibrary.sol";

contract InstantBuyOffering is Offering {
    using InstantBuyOfferingLibrary for InstantBuyOfferingLibrary.InstantBuyOffering;

    InstantBuyOfferingLibrary.InstantBuyOffering public instantBuyOffering;

    event onSettingsChanged(uint price);

    function InstantBuyOffering(
        address _ens,
        bytes32 _node,
        string _name,
        address _originalOwner,
        address _emergencyMultisig,
        uint _price
    )
        Offering(_ens, _node, _name, _originalOwner, _emergencyMultisig, 1, 1)
    {
        instantBuyOffering.price = _price;
    }

    function buy() payable {
        instantBuyOffering.buy(offering);
    }

    function setSettings(uint _price) {
        instantBuyOffering.setSettings(offering, _price);
    }

    function() payable {
        buy();
    }
}
