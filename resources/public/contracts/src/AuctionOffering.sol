pragma solidity ^0.4.14;

/**
 * @title AuctionOffering
 * @dev Extends Offering with Auction functionality
 * Logic for contract methods is at AuctionOfferingLibrary, so logic is not duplicated each time user creates
 * new offering. It saves large amounts of gas. AuctionOfferingLibrary address is linked to AuctionOffering contract
 * at compilation time.
 */

import "Offering.sol";
import "SafeMath.sol";
import "AuctionOfferingLibrary.sol";

contract AuctionOffering is Offering {
    using AuctionOfferingLibrary for AuctionOfferingLibrary.AuctionOffering;

    AuctionOfferingLibrary.AuctionOffering public auctionOffering;

    function AuctionOffering(
        address _offeringRegistry,
        address _registrar,
        bytes32 _node,
        string _name,
        bytes32 _labelHash,
        address _originalOwner,
        address _emergencyMultisig,
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    )
        Offering(_offeringRegistry, _registrar, _node, _name, _labelHash,  _originalOwner, _emergencyMultisig, 100000, _startPrice)
    {
        auctionOffering.construct(
            _endTime,
            _extensionDuration,
            _minBidIncrease
        );
    }

    function bid() payable {
        auctionOffering.bid(offering);
    }

    function finalize(bool transferPrice) {
        auctionOffering.finalize(offering, transferPrice);
    }

    function withdraw(address _address) {
        auctionOffering.withdraw(offering, _address);
    }

    function setSettings(
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        auctionOffering.setSettings(
            offering,
            _startPrice,
            _endTime,
            _extensionDuration,
            _minBidIncrease);
    }

    function reclaimOwnership() {
        auctionOffering.reclaimOwnership(offering);
    }

    function pendingReturns(address bidder) constant returns (uint) {
        return AuctionOfferingLibrary.pendingReturns(auctionOffering, bidder);
    }

    function() payable {
        bid();
    }
}

