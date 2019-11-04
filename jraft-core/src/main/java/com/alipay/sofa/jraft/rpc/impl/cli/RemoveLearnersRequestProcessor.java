/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.jraft.rpc.impl.cli;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executor;

import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.rpc.CliRequests.LearnersOpResponse;
import com.alipay.sofa.jraft.rpc.CliRequests.RemoveLearnersRequest;
import com.alipay.sofa.jraft.rpc.RpcRequestClosure;
import com.alipay.sofa.jraft.rpc.RpcResponseFactory;
import com.google.protobuf.Message;

/**
 * RemoveLearners request processor.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *
 */
public class RemoveLearnersRequestProcessor extends BaseCliRequestProcessor<RemoveLearnersRequest> {

    public RemoveLearnersRequestProcessor(final Executor executor) {
        super(executor);
    }

    @Override
    protected String getPeerId(final RemoveLearnersRequest request) {
        return request.getLeaderId();
    }

    @Override
    protected String getGroupId(final RemoveLearnersRequest request) {
        return request.getGroupId();
    }

    @Override
    protected Message processRequest0(final CliRequestContext ctx, final RemoveLearnersRequest request,
                                      final RpcRequestClosure done) {
        LinkedHashSet<PeerId> oldLearners = ctx.node.listLearners();
        List<PeerId> removeingLearners = new ArrayList<>(request.getLearnersCount());

        for (String peerStr : request.getLearnersList()) {
            PeerId peer = new PeerId();
            if (!peer.parse(peerStr)) {
                return RpcResponseFactory.newResponse(RaftError.EINVAL, "Fail to parse peer id %", peerStr);
            }
            removeingLearners.add(peer);
        }

        LOG.info("Receive RemoveLearnersRequest to {} from {}, removing {}", ctx.node.getNodeId(),
            done.getBizContext().getRemoteAddress(), removeingLearners);
        ctx.node.removeLearners(removeingLearners, status -> {
            if (!status.isOk()) {
                done.run(status);
            } else {
                LearnersOpResponse.Builder rb = LearnersOpResponse.newBuilder();

                for (PeerId peer : oldLearners) {
                    rb.addOldLearners(peer.toString());
                    if (!removeingLearners.contains(peer)) {
                        rb.addNewLearners(peer.toString());
                    }
                }

                done.sendResponse(rb.build());
            }
        });

        return null;
    }

    @Override
    public String interest() {
        return RemoveLearnersRequest.class.getName();
    }

}