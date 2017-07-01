pragma solidity ^0.4.11;

import "Offering.sol";
import "SafeMath.sol";

contract EnglishAuctionOffering is Offering {
    using SafeMath for uint;

    uint8 public offeringType = 2;

    uint public startPrice;
    uint public startTime;
    uint public endTime;
    uint public extensionDuration;
    uint public extensionTriggerDuration;
    uint public minBidIncrease;

    address highestBidder;

    struct Bid {
        uint amount;
        uint updatedOn;
    }

    mapping(address => Bid) public bids;

    event onBid(address indexed bidder, uint amount, uint endTime, uint datetime);

    function EnglishAuctionOffering(
        address _ens,
        bytes32 _node,
        string _name,
        address _originalOwner,
        uint _startPrice,
        uint _startTime,
        uint _endTime,
        uint _extensionDuration,
        uint _extensionTriggerDuration,
        uint _minBidIncrease
    )
        Offering(_ens, _node, _name, _originalOwner)
    {
        startPrice = _startPrice;
        startTime = _startTime < now ? now : startTime;
        require(endTime > startTime);
        endTime = _endTime;
        extensionDuration = _extensionDuration;
        extensionTriggerDuration = _extensionTriggerDuration;
        require(minBidIncrease > 0);
        minBidIncrease = _minBidIncrease;
    }

    function bid()
        payable
        contractOwnsNode
    {
        require(startTime >= now);
        require(now < endTime);

        if (bids[highestBidder].amount == 0) {
            require(msg.value >= startPrice);
        } else {
            require(msg.value >= bids[highestBidder].amount.add(minBidIncrease));
            highestBidder.transfer(bids[highestBidder].amount);
        }

        highestBidder = msg.sender;

        if (bids[msg.sender].amount == 0) {
            bids[msg.sender] = Bid(msg.value, now);
        } else {
            bids[msg.sender].amount = msg.value;
            bids[msg.sender].updatedOn = now;
        }

        if ((endTime - extensionTriggerDuration) <= now) {
            endTime = now.add(extensionDuration);
        }

        onBid(msg.sender, bids[msg.sender].amount, endTime, now);
    }

    function finalize()
        contractOwnsNode
    {
        require(now > endTime);
        require(highestBidder != 0x0);
        originalOwner.transfer(bids[highestBidder].amount);
        ens.setOwner(node, highestBidder);
        newOwner = highestBidder;
        transferredOn = now;
        onTransfer(newOwner, bids[highestBidder].amount, now);
    }

    function cancel()
        onlyOriginalOwner
        contractOwnsNode
    {
        require(now < endTime);
        require(highestBidder == 0x0);
        ens.setOwner(node, originalOwner);
        onCancel(now);
    }
}
