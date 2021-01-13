pragma solidity ^0.4.18;

import "ens-repo/contracts/ENSRegistry.sol";
import "ens-repo/contracts/HashRegistrar.sol";
import "OfferingRegistry.sol";
import "OfferingRequestsAbstract.sol";
import "strings-lib-repo/strings.sol";

/**
 * @title OfferingFactory
 * @dev Base contract factory for creating new offerings
 */

contract OfferingFactory {
    using strings for *;

    ENSRegistry public ens;
    OfferingRegistry public offeringRegistry;
    OfferingRequestsAbstract public offeringRequests;

    // Hardcoded namehash of "eth"
    bytes32 public constant rootNode = 0x93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae;

    function OfferingFactory (
        ENSRegistry _ens,
        OfferingRegistry _offeringRegistry,
        OfferingRequestsAbstract _offeringRequests
    ) {
        ens = _ens;
        offeringRegistry = _offeringRegistry;
        offeringRequests = _offeringRequests;
    }

    /**
    * @dev Registers new offering to OfferingRegistry, clears offering requests for this ENS node
    * Must check if creator of offering is actual owner of ENS name and for top level names also deed owner
    * @param node bytes32 ENS node
    * @param labelHash bytes32 ENS labelhash
    * @param newOffering address The address of new offering
    * @param version uint The version of offering contract
    */
    function registerOffering(bytes32 node, bytes32 labelHash, address newOffering, uint version)
        internal
    {
        require(ens.owner(node) == msg.sender);
        if (node == sha3(rootNode, labelHash)) {
            address deed;
            (,deed,,,) = HashRegistrar(ens.owner(rootNode)).entries(labelHash);
            require(Deed(deed).owner() == msg.sender);
        }

        offeringRegistry.addOffering(newOffering, node, msg.sender, version);
        offeringRequests.clearRequests(node);
    }

    /**
    * @dev Namehash function used by ENS to convert plain text name into node hash
    * @param name string Plaintext ENS name
    * @return bytes32 ENS node hash, aka node
    */
    function namehash(string name) internal returns(bytes32) {
        var nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        var label = nameSlice.split(".".toSlice()).toString();
        return sha3(namehash(nameSlice.toString()), sha3(label));
    }

    /**
    * @dev Calculates labelHash of ENS name
    * @param name string Plaintext ENS name
    * @return bytes32 ENS labelHash, hashed label of the name
    */
    function getLabelHash(string name) internal returns(bytes32) {
        return sha3(name.toSlice().split(".".toSlice()).toString());
    }
}