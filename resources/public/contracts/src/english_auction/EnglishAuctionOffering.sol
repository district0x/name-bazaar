pragma solidity ^0.4.11;

import "Offering.sol";
import "SafeMath.sol";
import "./EnglishAuctionOfferingLibrary.sol";

contract EnglishAuctionOffering is Offering {
    using EnglishAuctionOfferingLibrary for EnglishAuctionOfferingLibrary.EnglishAuctionOffering;

    EnglishAuctionOfferingLibrary.EnglishAuctionOffering public englishAuctionOffering;

    event onBid(address indexed bidder, uint amount, uint endTime, uint datetime);

    function EnglishAuctionOffering(
        address _offeringRegistry,
        address _ens,
        bytes32 _node,
        string _name,
        address _originalOwner,
        address _emergencyMultisig,
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    )
        Offering(_offeringRegistry, _ens, _node, _name, _originalOwner, _emergencyMultisig, 100000, _startPrice)
    {
        englishAuctionOffering.construct(
            _endTime,
            _extensionDuration,
            _minBidIncrease
        );
    }

    function bid() payable {
        englishAuctionOffering.bid(offering);
    }

    function finalize() {
        englishAuctionOffering.finalize(offering);
    }

    function setSettings(
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        englishAuctionOffering.setSettings(
            offering,
            _startPrice,
            _endTime,
            _extensionDuration,
            _minBidIncrease);
    }

    function reclaim() {
        englishAuctionOffering.reclaim(offering);
    }

    function() payable {
        bid();
    }
}

