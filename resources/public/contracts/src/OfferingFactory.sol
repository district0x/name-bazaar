pragma solidity ^0.4.11;

import "ens/ENS.sol";
import "OfferingRegistry.sol";
import "OfferingRequests.sol";
import "strings.sol";

contract OfferingFactory {
    using strings for *;

    ENS public ens;
    OfferingRegistry public offeringRegistry;
    OfferingRequests public offeringRequests;

    function OfferingFactory(
        address _ens,
        address _offeringRegistry,
        address _offeringRequests
    ) {
        ens = ENS(_ens);
        offeringRegistry = OfferingRegistry(_offeringRegistry);
        offeringRequests = OfferingRequests(_offeringRequests);
    }

    function registerOffering(bytes32 node, address newOffering)
        internal
    {
        ens.setOwner(node, newOffering);
        offeringRegistry.addOffering(newOffering);
        offeringRequests.clearRequests(node);
    }

    function namehash(string name) internal returns(bytes32) {
        var nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        var label = nameSlice.split(".".toSlice()).toString();
        return sha3(namehash(nameSlice.toString()), sha3(label));
    }
}