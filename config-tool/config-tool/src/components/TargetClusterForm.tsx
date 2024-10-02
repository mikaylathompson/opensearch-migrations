import React from 'react';
import {
  Container,
  FormField,
  Header,
  Input,
  RadioGroup,
  Select,
} from "@cloudscape-design/components";

type TargetClusterFormProps = {
  formValues: any;
  partialStateUpdate: (update: Partial<any>) => void;
  regionList: string[];
};

const TargetClusterForm: React.FC<TargetClusterFormProps> = ({
  formValues,
  partialStateUpdate,
  regionList,
}) => {
  return (
    <Container header={<Header variant="h2">Target Cluster</Header>}>
      <FormField
        label="Endpoint"
        description="The endpoint, including port if necessary, of the destination cluster."
      >
        <Input
          value={formValues?.targetClusterEndpoint ?? ""}
          onChange={({ detail }) =>
            partialStateUpdate({
              targetClusterEndpoint: detail.value,
            })
          }
        />
      </FormField>
      <FormField
        label="Auth"
        description="The authorization type of the destination cluster."
      >
        <RadioGroup
          onChange={({ detail }) => {
            partialStateUpdate({
              targetClusterAuthType: detail.value,
              targetClusterAuthUsername: undefined,
              targetClusterAuthPasswordFromArn: undefined,
              targetClusterAuthServiceSigningName: undefined,
              targetClusterAuthRegion: undefined,
            });
          }}
          value={formValues?.targetClusterAuthType}
          items={[
            { value: "none", label: "No authorization" },
            {
              value: "basic",
              label: "Basic authorization with a username and password",
            },
            { value: "sigv4", label: "SigV4 signing" },
          ]}
        />
      </FormField>
      {formValues.targetClusterAuthType === "basic" && (
        <Container>
          <FormField
            label="Username"
            description="Username of the master user on the destination cluster"
          >
            <Input
              value={formValues?.targetClusterAuthUsername ?? ""}
              onChange={({ detail }) =>
                partialStateUpdate({
                  targetClusterAuthUsername: detail.value,
                })
              }
            />
          </FormField>
          <FormField
            label="Password from Secret ARN"
            description="ARN of a Secrets Manager secret containing the password of the master user on the destination cluster"
          >
            <Input
              value={formValues?.targetClusterAuthPasswordFromArn ?? ""}
              onChange={({ detail }) =>
                partialStateUpdate({
                  targetClusterAuthPasswordFromArn: detail.value,
                })
              }
            />
          </FormField>
        </Container>
      )}
      {formValues.targetClusterAuthType === "sigv4" && (
        <Container>
          <FormField
            label="Service Signing Name"
            description="The name of the service to use for SigV4 signing"
          >
            <RadioGroup
              onChange={({ detail }) =>
                partialStateUpdate({
                  targetClusterAuthServiceSigningName: detail.value,
                })
              }
              value={formValues?.targetClusterAuthServiceSigningName ?? ""}
              items={[
                {
                  value: "es",
                  label: "Amazon Elasticsearch and OpenSearch Managed Service",
                },
                {
                  value: "aoss",
                  label: "Amazon OpenSearch Serverless Service",
                },
              ]}
            />
          </FormField>
          <FormField
            label="Region"
            description="The region of the destination cluster"
          >
            <Select
              selectedOption={{
                value: formValues.targetClusterAuthRegion,
              }}
              onChange={({ detail }) =>
                partialStateUpdate({
                  targetClusterAuthRegion: detail.selectedOption.value,
                })
              }
              options={regionList.map((r) => ({
                value: r,
                label: r,
              }))}
            />
          </FormField>
        </Container>
      )}
    </Container>
  );
};

export default TargetClusterForm;