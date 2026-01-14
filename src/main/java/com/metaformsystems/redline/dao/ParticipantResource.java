package com.metaformsystems.redline.dao;

import java.util.List;

/**
 *
 */
public record ParticipantResource(Long id,
                                  String identifier,
                                  List<VPAResource> agents,
                                  List<DataspaceInfo> infos) {

}
