pragma solidity ^0.4.11;

import "ens/AbstractENS.sol";
import "OfferingLibrary.sol";

contract Offering {
    using OfferingLibrary for OfferingLibrary.Offering;

    OfferingLibrary.Offering public offering;

    event onReclaim(uint datetime, bool isEmergency);
    event onTransfer(address newOwner, uint price, uint datetime);

    function Offering(
        address _ens,
        bytes32 _node,
        string _name,
        address _originalOwner,
        address _emergencyMultisig,
        uint _offeringType,
        uint _contractVersion
    ) {
        offering.construct(
            _ens,
            _node,
            _name,
            _originalOwner,
            _emergencyMultisig,
            _offeringType,
            _contractVersion
        );
    }

    function reclaim() {
        offering.reclaim();
    }

    function claim(bytes32 node, address claimer) {
        offering.claim(node, claimer);
    }
}
