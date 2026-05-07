FROM public.ecr.aws/lambda/python:3.12

COPY requirements.txt ${LAMBDA_TASK_ROOT}/
RUN pip install --no-cache-dir --target ${LAMBDA_TASK_ROOT} -r ${LAMBDA_TASK_ROOT}/requirements.txt

COPY processor.py lambda_function.py calibrador_global_SP2VC_features_stack.joblib ${LAMBDA_TASK_ROOT}/

CMD ["lambda_function.lambda_handler"]
