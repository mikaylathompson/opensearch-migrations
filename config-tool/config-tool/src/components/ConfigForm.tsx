import React from 'react';
import {
  SpaceBetween,
  FormField,
  Input,
  Checkbox,
} from "@cloudscape-design/components";
import SourceClusterForm from './SourceClusterForm';
import TargetClusterForm from './TargetClusterForm';

type ConfigFormProps = {
  formValues: any;
  partialStateUpdate: (update: Partial<any>) => void;
  regionList: string[];
};

const ConfigForm: React.FC<ConfigFormProps> = ({
  formValues,
  partialStateUpdate,
  regionList,
}) => {
  return (
    <SpaceBetween direction="vertical" size="l">
      <FormField label="VPC Id">
        <Input
          value={formValues?.vpcId ?? ""}
          onChange={({ detail }) =>
            partialStateUpdate({ vpcId: detail.value })
          }
        />
      </FormField>
      <FormField label="Use Reindex-From-Snapshot?">
        <Checkbox
          onChange={({ detail }) =>
            partialStateUpdate({ useRfs: detail.checked })
          }
          checked={formValues?.useRfs ?? false}
          description="Reindex-From-Snapshot loads existing data from the cluster, via a snapshot. Enabling it will configure a snapshot, metadata migration, and document backfill."
        />
      </FormField>
      <FormField label="Use Traffic Capture & Replay">
        <Checkbox
          onChange={({ detail }) =>
            partialStateUpdate({ useReplayer: detail.checked })
          }
          checked={formValues?.useReplayer ?? false}
          description="Traffic Capture relies on a capture proxy (deployed in front of your existing cluster) and replayer. It provides live traffic migration & validation."
        />
      </FormField>
      <SourceClusterForm
        formValues={formValues}
        partialStateUpdate={partialStateUpdate}
        regionList={regionList}
      />
      <TargetClusterForm
        formValues={formValues}
        partialStateUpdate={partialStateUpdate}
        regionList={regionList}
      />
    </SpaceBetween>
  );
};

export default ConfigForm;